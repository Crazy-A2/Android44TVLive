package com.tvlive.app.ui.presenter

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import com.tvlive.app.TvliveApp
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.repository.SourceRepository
import com.tvlive.app.player.PlayerManager
import com.tvlive.app.ui.activity.LivePlayerActivity
import com.tvlive.app.util.PreferenceHelper

class LivePlayerPresenter(
    private val activity: LivePlayerActivity,
    private val playerManager: PlayerManager,
    private val prefs: PreferenceHelper
) {

    private val handler = Handler()
    private val sourceRepository = SourceRepository(TvliveApp.db.sourceDao())
    private val audioManager: AudioManager =
        activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var channels: List<Channel> = emptyList()
    private var currentIndex: Int = -1
    private var currentSourceId: Long = -1L
    private var isReady = false

    private val numberInputBuffer = StringBuilder()
    private var numberInputRunnable: Runnable? = null

    fun init() {
        Thread {
            channels = TvliveApp.db.channelDao().getAllOrdered()
            currentIndex = channels.indexOfFirst { it.id == prefs.lastChannelId }
            if (currentIndex == -1 && channels.isNotEmpty()) {
                currentIndex = 0
            }
            isReady = true
        }.start()
    }

    fun switchChannel(direction: Int) {
        if (!isReady || channels.isEmpty()) return
        val newIndex = when {
            direction > 0 -> (currentIndex + 1) % channels.size
            direction < 0 -> (currentIndex - 1 + channels.size) % channels.size
            else -> return
        }
        playChannel(channels[newIndex])
    }

    fun playChannel(channel: Channel?) {
        channel ?: return
        currentIndex = channels.indexOfFirst { it.id == channel.id }
        prefs.lastChannelId = channel.id
        Thread {
            val source = sourceRepository.getBestSource(channel.id)
            activity.runOnUiThread {
                if (source != null) {
                    currentSourceId = source.id
                    playerManager.play(source.url)
                    activity.showChannelInfo(channel)
                } else {
                    activity.showStatusMessage("暂无可用源")
                }
            }
        }.start()
    }

    fun adjustVolume(direction: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current + direction).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        val percent = newVolume * 100 / max
        prefs.volumeLevel = percent
        activity.showVolumeBar(percent)
    }

    fun toggleMute() {
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, !isMuted())
        activity.showStatusMessage(if (isMuted()) "已静音" else "已取消静音")
    }

    fun isMuted(): Boolean = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)

    fun startNumberInput(digit: Int) {
        numberInputBuffer.append(digit)
        val input = numberInputBuffer.toString()
        activity.showChannelNumberInput(input)

        numberInputRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            val number = numberInputBuffer.toString().toIntOrNull() ?: return@Runnable
            numberInputBuffer.clear()
            activity.hideChannelNumberInput()
            val target = channels.find { it.channelNumber == number }
            if (target != null) {
                playChannel(target)
            } else {
                activity.showStatusMessage("频道不存在")
            }
        }
        numberInputRunnable = runnable
        handler.postDelayed(runnable, 2000)
    }

    fun cancelNumberInput() {
        numberInputRunnable?.let { handler.removeCallbacks(it) }
        numberInputBuffer.clear()
        activity.hideChannelNumberInput()
    }

    fun onPlaybackError() {
        val channel = getCurrentChannel() ?: return
        if (currentSourceId == -1L) return
        Thread {
            sourceRepository.reportSourceFailed(currentSourceId)
            val next = sourceRepository.getNextAvailableSource(channel.id, currentSourceId)
            activity.runOnUiThread {
                if (next != null) {
                    currentSourceId = next.id
                    playerManager.play(next.url)
                    activity.showStatusMessage("正在切换源...")
                } else {
                    activity.showStatusMessage("该频道暂无可用源")
                }
            }
        }.start()
    }

    fun onPlaybackPrepared() {
        if (currentSourceId != -1L) {
            val responseTimeMs = 0 // 播放器未暴露精确的响应时间，暂不调整
            sourceRepository.reportSourceSuccess(currentSourceId, responseTimeMs)
        }
    }

    fun getCurrentChannel(): Channel? {
        if (currentIndex in channels.indices) return channels[currentIndex]
        return null
    }

    fun getChannels(): List<Channel> = channels
}
