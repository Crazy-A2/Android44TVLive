package com.tvlive.app.ui.presenter

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Source
import com.tvlive.app.data.repository.SourceRepository
import com.tvlive.app.player.PlayerManager
import com.tvlive.app.ui.activity.LivePlayerActivity
import com.tvlive.app.util.PreferenceHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.*

class LivePlayerPresenterTest {

    private lateinit var activity: LivePlayerActivity
    private lateinit var playerManager: PlayerManager
    private lateinit var prefs: PreferenceHelper
    private lateinit var sourceRepo: SourceRepository
    private lateinit var audioManager: AudioManager
    private lateinit var handler: Handler
    private lateinit var presenter: LivePlayerPresenter

    @Before
    fun setUp() {
        activity = mock(LivePlayerActivity::class.java)
        playerManager = mock(PlayerManager::class.java)
        prefs = mock(PreferenceHelper::class.java)
        sourceRepo = mock(SourceRepository::class.java)
        audioManager = mock(AudioManager::class.java)
        handler = mock(Handler::class.java)
        `when`(activity.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)

        presenter = LivePlayerPresenter(activity, playerManager, prefs, sourceRepo, handler)
    }

    // --- startNumberInput ---

    @Test
    fun `startNumberInput collects digit and shows input`() {
        presenter.startNumberInput(5)
        verify(activity).showChannelNumberInput("5")

        presenter.startNumberInput(1)
        verify(activity).showChannelNumberInput("51")
    }

    @Test
    fun `startNumberInput schedules timeout`() {
        presenter.startNumberInput(5)
        verify(handler).postDelayed(any<Runnable>(), eq(2000L))
    }

    @Test
    fun `startNumberInput cancels previous timeout before posting new one`() {
        presenter.startNumberInput(5)
        presenter.startNumberInput(1)
        // 两次 postDelayed = 1次移除 + 2次提交（第一次post时没runnable，第二次移除第一次的再post新的）
        verify(handler, atMost(2)).removeCallbacks(any<Runnable>())
        verify(handler, times(2)).postDelayed(any<Runnable>(), eq(2000L))
    }

    @Test
    fun `startNumberInput valid channel triggers play on timeout`() {
        val channels = listOf(Channel(id = 1, channelNumber = 51, name = "Test"))
        presenter.channels = channels
        presenter.isReady = true

        // 捕获 postDelayed 的 runnable
        val posted = mutableListOf<Runnable>()
        doAnswer { inv ->
            posted.add(inv.getArgument<Runnable>(0))
            null
        }.`when`(handler).postDelayed(any<Runnable>(), anyLong())

        presenter.startNumberInput(5)
        presenter.startNumberInput(1)

        // 模拟超时执行
        posted.lastOrNull()?.run()

        // 应该调用 hideChannelNumberInput
        verify(activity, atLeastOnce()).hideChannelNumberInput()
    }

    @Test
    fun `startNumberInput non-existent channel shows error on timeout`() {
        val channels = listOf(Channel(id = 1, channelNumber = 1, name = "Test"))
        presenter.channels = channels
        presenter.isReady = true

        val posted = mutableListOf<Runnable>()
        doAnswer { inv ->
            posted.add(inv.getArgument<Runnable>(0))
            null
        }.`when`(handler).postDelayed(any<Runnable>(), anyLong())

        presenter.startNumberInput(9)
        presenter.startNumberInput(9)

        posted.lastOrNull()?.run()
        verify(activity).showStatusMessage("频道不存在")
    }

    @Test
    fun `cancelNumberInput clears buffer and hides input`() {
        presenter.startNumberInput(5)
        presenter.cancelNumberInput()
        verify(handler).removeCallbacks(any<Runnable>())
        verify(activity).hideChannelNumberInput()
    }

    @Test
    fun `cancelNumberInput cancels pending timeout`() {
        presenter.startNumberInput(5)
        presenter.cancelNumberInput()
        verify(handler).removeCallbacks(any<Runnable>())
    }

    // --- onPlaybackPrepared ---

    @Test
    fun `onPlaybackPrepared reports success when source id set`() {
        presenter.currentSourceId = 1L
        presenter.onPlaybackPrepared()
        verify(sourceRepo).reportSourceSuccess(eq(1L), anyInt())
    }

    @Test
    fun `onPlaybackPrepared skips when no source id`() {
        presenter.currentSourceId = -1L
        presenter.onPlaybackPrepared()
        verify(sourceRepo, never()).reportSourceSuccess(anyLong(), anyInt())
    }

    @Test
    fun `onPlaybackPrepared reports zero when playStartTime not set`() {
        presenter.currentSourceId = 1L
        presenter.onPlaybackPrepared()
        verify(sourceRepo).reportSourceSuccess(eq(1L), eq(0))
    }

    // --- onPlaybackError ---

    @Test
    fun `onPlaybackError switches to next available source`() {
        val channel = Channel(id = 1, channelNumber = 1, name = "Test")
        val nextSource = Source(id = 2, channelId = 1, url = "http://next", streamType = "hls")
        presenter.channels = listOf(channel)
        presenter.currentIndex = 0
        presenter.currentSourceId = 1L

        `when`(sourceRepo.reportSourceFailed(1L)).thenReturn(null)
        `when`(sourceRepo.getNextAvailableSource(1L, 1L)).thenReturn(nextSource)

        doAnswer { inv ->
            (inv.getArgument<Runnable>(0)).run()
            null
        }.`when`(activity).runOnUiThread(any())

        presenter.onPlaybackError()

        verify(sourceRepo, timeout(1000)).reportSourceFailed(1L)
        verify(sourceRepo, timeout(1000)).getNextAvailableSource(1L, 1L)
        verify(playerManager, timeout(1000)).play("http://next")
        verify(activity, timeout(1000)).showStatusMessage("正在切换源...")
    }

    @Test
    fun `onPlaybackError skips when no current channel`() {
        presenter.currentSourceId = 1L
        presenter.onPlaybackError()
        Thread.sleep(100)
        verify(sourceRepo, never()).reportSourceFailed(anyLong())
    }

    @Test
    fun `onPlaybackError skips when no source id`() {
        presenter.channels = listOf(Channel(id = 1, channelNumber = 1, name = "Test"))
        presenter.currentIndex = 0
        presenter.currentSourceId = -1L
        presenter.onPlaybackError()
        Thread.sleep(100)
        verify(sourceRepo, never()).reportSourceFailed(anyLong())
    }

    @Test
    fun `onPlaybackError triggers source update when no next source`() {
        val channel = Channel(id = 1, channelNumber = 1, name = "Test")
        presenter.channels = listOf(channel)
        presenter.currentIndex = 0
        presenter.currentSourceId = 1L

        `when`(sourceRepo.reportSourceFailed(1L)).thenReturn(null)
        `when`(sourceRepo.getNextAvailableSource(1L, 1L)).thenReturn(null)

        var onNoSourcesCalled = false
        presenter.onNoSourcesAvailable = { onNoSourcesCalled = true }

        doAnswer { inv ->
            (inv.getArgument<Runnable>(0)).run()
            null
        }.`when`(activity).runOnUiThread(any())

        presenter.onPlaybackError()

        verify(activity, timeout(1000)).showStatusMessage("该频道暂无可用源，正在更新...")
        assertTrue(onNoSourcesCalled)
    }

    @Test
    fun `onPlaybackError updates currentSourceId when no next source`() {
        val channel = Channel(id = 1, channelNumber = 1, name = "Test")
        presenter.channels = listOf(channel)
        presenter.currentIndex = 0
        presenter.currentSourceId = 1L

        `when`(sourceRepo.reportSourceFailed(1L)).thenReturn(null)
        `when`(sourceRepo.getNextAvailableSource(1L, 1L)).thenReturn(null)

        doAnswer { inv ->
            (inv.getArgument<Runnable>(0)).run()
            null
        }.`when`(activity).runOnUiThread(any())

        presenter.onPlaybackError()

        verify(activity, timeout(1000)).showStatusMessage("该频道暂无可用源，正在更新...")
        assertEquals(-1L, presenter.currentSourceId)
    }

    // --- adjustVolume ---

    @Test
    fun `adjustVolume increases volume`() {
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(10)
        presenter.adjustVolume(1)
        verify(audioManager).setStreamVolume(AudioManager.STREAM_MUSIC, 11, 0)
        verify(activity).showVolumeBar(73)
    }

    @Test
    fun `adjustVolume decreases volume`() {
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(10)
        presenter.adjustVolume(-1)
        verify(audioManager).setStreamVolume(AudioManager.STREAM_MUSIC, 9, 0)
        verify(activity).showVolumeBar(60)
    }

    @Test
    fun `adjustVolume clamps to minimum zero`() {
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(0)
        presenter.adjustVolume(-1)
        verify(audioManager).setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    @Test
    fun `adjustVolume clamps to maximum`() {
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        presenter.adjustVolume(1)
        verify(audioManager).setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0)
    }

    // --- isMuted / toggleMute ---

    @Test
    fun `isMuted delegates to audioManager`() {
        `when`(audioManager.isStreamMute(AudioManager.STREAM_MUSIC)).thenReturn(true)
        assertTrue(presenter.isMuted())

        `when`(audioManager.isStreamMute(AudioManager.STREAM_MUSIC)).thenReturn(false)
        assertFalse(presenter.isMuted())
    }

    @Test
    fun `toggleMute switches mute state`() {
        presenter.toggleMute()
        verify(audioManager).setStreamMute(AudioManager.STREAM_MUSIC, true)
    }

    // --- getCurrentChannel / getChannels ---

    @Test
    fun `getCurrentChannel returns null when no channels`() {
        assertNull(presenter.getCurrentChannel())
    }

    @Test
    fun `getChannels returns channels`() {
        val channels = listOf(
            Channel(id = 1, channelNumber = 1, name = "A"),
            Channel(id = 2, channelNumber = 2, name = "B")
        )
        presenter.channels = channels
        assertEquals(2, presenter.getChannels().size)
    }
}
