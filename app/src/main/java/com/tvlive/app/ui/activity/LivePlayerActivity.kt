package com.tvlive.app.ui.activity

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tvlive.app.R
import com.tvlive.app.TvliveApp
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Source
import com.tvlive.app.data.parser.M3uParser
import com.tvlive.app.player.PlayerCallback
import com.tvlive.app.player.PlayerManager
import com.tvlive.app.ui.osd.ChannelInfoBar
import com.tvlive.app.ui.osd.ChannelListOverlay
import com.tvlive.app.ui.osd.ChannelNumberInput
import com.tvlive.app.ui.osd.OsdManager
import com.tvlive.app.ui.osd.SettingsOverlay
import com.tvlive.app.ui.osd.VolumeBar
import com.tvlive.app.ui.presenter.ChannelListPresenter
import com.tvlive.app.ui.presenter.SettingsPresenter
import com.tvlive.app.ui.presenter.LivePlayerPresenter
import com.tvlive.app.util.PreferenceHelper

class LivePlayerActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var channelInfoBarContainer: FrameLayout
    private lateinit var volumeBarContainer: FrameLayout
    private lateinit var channelNumberInputContainer: FrameLayout
    private lateinit var channelListOverlayContainer: FrameLayout
    private lateinit var settingsOverlay: FrameLayout
    private lateinit var statusMessage: TextView

    private lateinit var playerManager: PlayerManager
    private lateinit var presenter: LivePlayerPresenter
    private lateinit var channelListPresenter: ChannelListPresenter
    private lateinit var channelNumberInput: ChannelNumberInput
    private lateinit var channelInfoBar: ChannelInfoBar
    private lateinit var volumeBar: VolumeBar
    private lateinit var channelListOverlay: ChannelListOverlay
    private lateinit var settingsOverlayObj: SettingsOverlay
    private lateinit var settingsPresenter: SettingsPresenter
    private lateinit var osdManager: OsdManager
    private lateinit var prefs: PreferenceHelper
    private val handler = Handler()
    private var hideStatusRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_player)

        root = findViewById(R.id.root)
        surfaceView = findViewById(R.id.surface_view)
        channelInfoBarContainer = findViewById(R.id.channel_info_bar)
        volumeBarContainer = findViewById(R.id.volume_bar_container)
        channelNumberInputContainer = findViewById(R.id.channel_number_input_container)
        channelListOverlayContainer = findViewById(R.id.channel_list_overlay)
        settingsOverlay = findViewById(R.id.settings_overlay)
        statusMessage = findViewById(R.id.status_message)

        prefs = PreferenceHelper(this)
        channelNumberInput = ChannelNumberInput(channelNumberInputContainer)
        channelInfoBar = ChannelInfoBar(channelInfoBarContainer)
        volumeBar = VolumeBar(volumeBarContainer)

        channelListPresenter = ChannelListPresenter(
            TvliveApp.db.channelDao(),
            TvliveApp.db.favoriteDao(),
            TvliveApp.db.sourceDao()
        )

        osdManager = OsdManager(handler)
        osdManager.onStateChanged = { state ->
            when (state) {
                OsdManager.OsdState.CHANNEL_INFO -> channelInfoBar.show(
                    presenter.getCurrentChannel()?.channelNumber ?: 0,
                    presenter.getCurrentChannel()?.name ?: ""
                )
                OsdManager.OsdState.VOLUME -> {
                    // VolumeBar 由 showVolumeBar 直接控制显示
                }
                OsdManager.OsdState.IDLE -> {
                    channelInfoBar.hide()
                    volumeBar.hide()
                    if (::channelListOverlay.isInitialized && channelListOverlay.isVisible()) {
                        channelListOverlay.close()
                    }
                    if (::settingsOverlayObj.isInitialized && settingsOverlayObj.isVisible()) {
                        settingsOverlayObj.close()
                    }
                }
                else -> {}
            }
        }

        channelListOverlay = ChannelListOverlay(
            container = channelListOverlayContainer,
            presenter = channelListPresenter,
            osdManager = osdManager,
            onChannelSelected = { channel ->
                presenter.playChannel(channel)
                osdManager.hide()
            },
            getCurrentChannelId = { presenter.getCurrentChannel()?.id ?: -1L }
        )

        settingsPresenter = SettingsPresenter(
            TvliveApp.db.sourceConfigDao(),
            prefs
        )
        settingsOverlayObj = SettingsOverlay(
            container = settingsOverlay,
            presenter = settingsPresenter,
            osdManager = osdManager
        )

        presenter = LivePlayerPresenter(this, playerManager, prefs)
        presenter.init()
        loadAndPlay()
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        playerManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                presenter.switchChannel(-1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                presenter.switchChannel(1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                presenter.adjustVolume(-1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                presenter.adjustVolume(1)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                presenter.adjustVolume(1)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                presenter.adjustVolume(-1)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                presenter.toggleMute()
                return true
            }
            KeyEvent.KEYCODE_0 -> {
                presenter.startNumberInput(0)
                return true
            }
            KeyEvent.KEYCODE_1 -> {
                presenter.startNumberInput(1)
                return true
            }
            KeyEvent.KEYCODE_2 -> {
                presenter.startNumberInput(2)
                return true
            }
            KeyEvent.KEYCODE_3 -> {
                presenter.startNumberInput(3)
                return true
            }
            KeyEvent.KEYCODE_4 -> {
                presenter.startNumberInput(4)
                return true
            }
            KeyEvent.KEYCODE_5 -> {
                presenter.startNumberInput(5)
                return true
            }
            KeyEvent.KEYCODE_6 -> {
                presenter.startNumberInput(6)
                return true
            }
            KeyEvent.KEYCODE_7 -> {
                presenter.startNumberInput(7)
                return true
            }
            KeyEvent.KEYCODE_8 -> {
                presenter.startNumberInput(8)
                return true
            }
            KeyEvent.KEYCODE_9 -> {
                presenter.startNumberInput(9)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!channelListOverlay.isVisible()) {
                    channelListPresenter.init()
                    channelListOverlay.show()
                } else {
                    channelListOverlay.close()
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                settingsOverlayObj.show()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (channelNumberInputContainer.visibility == View.VISIBLE) {
                    presenter.cancelNumberInput()
                    return true
                }
                if (channelListOverlay.isVisible()) {
                    channelListOverlay.close()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // 长按 OK：收藏/取消收藏（步骤 5.1 实现）
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onBackPressed() {
        if (channelNumberInputContainer.visibility == View.VISIBLE) {
            presenter.cancelNumberInput()
            return
        }
        if (channelListOverlay.isVisible()) {
            channelListOverlay.close()
            return
        }
        if (settingsOverlayObj.isVisible()) {
            settingsOverlayObj.close()
            return
        }
        super.onBackPressed()
    }

    private fun initPlayer() {
        playerManager = PlayerManager()
        playerManager.init(surfaceView)
        playerManager.setCallback(object : PlayerCallback {
            override fun onPrepared() {
                hideStatusMessage()
                presenter.onPlaybackPrepared()
            }

            override fun onError(what: Int, extra: Int) {
                showStatusMessage("播放失败")
                presenter.onPlaybackError()
            }

            override fun onInfo(what: Int, extra: Int) {
                when (what) {
                    PlayerManager.MEDIA_INFO_BUFFERING_START -> showStatusMessage("加载中...")
                    PlayerManager.MEDIA_INFO_BUFFERING_END -> hideStatusMessage()
                }
            }

            override fun onVideoSizeChanged(width: Int, height: Int) {}
        })
    }

    private fun loadAndPlay() {
        showStatusMessage("加载中...")
        Thread {
            val db = TvliveApp.db
            if (db.channelDao().count() == 0) {
                loadBuiltinSources()
            }
            val channels = db.channelDao().getAllOrdered()
            if (channels.isEmpty()) {
                runOnUiThread { showStatusMessage("暂无频道") }
                return@Thread
            }
            val lastId = prefs.lastChannelId
            val channel = if (lastId != -1L) db.channelDao().getById(lastId) else null
                ?: channels.first()
            val source = db.sourceDao().getBestSource(channel.id)
            runOnUiThread {
                if (source != null) {
                    playerManager.play(source.url)
                    prefs.lastChannelId = channel.id
                } else {
                    showStatusMessage("暂无可用源")
                }
            }
        }.start()
    }

    private fun loadBuiltinSources() {
        try {
            assets.open("builtin_sources.m3u").use { stream ->
                val result = M3uParser.parse(stream)
                val db = TvliveApp.db
                var channelNumber = 1
                for (pc in result.channels) {
                    val channel = Channel(
                        channelNumber = channelNumber++,
                        name = pc.name,
                        category = pc.category,
                        logoUrl = pc.logoUrl,
                        epgId = pc.epgId
                    )
                    val cid = db.channelDao().insert(channel)
                    val sources = pc.sources.map { ps ->
                        Source(
                            channelId = cid,
                            url = ps.url,
                            streamType = ps.streamType,
                            quality = ps.quality,
                            provider = ps.provider
                        )
                    }
                    db.sourceDao().insertAll(sources)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showStatusMessage(text: String, autoHideMs: Long = 0) {
        statusMessage.text = text
        statusMessage.visibility = View.VISIBLE
        hideStatusRunnable?.let { handler.removeCallbacks(it) }
        hideStatusRunnable = null
        if (autoHideMs > 0) {
            val runnable = Runnable { hideStatusMessage() }
            hideStatusRunnable = runnable
            handler.postDelayed(runnable, autoHideMs)
        }
    }

    fun hideStatusMessage() {
        statusMessage.visibility = View.GONE
        hideStatusRunnable = null
    }

    fun showChannelInfo(channel: Channel) {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO, 3000)
    }

    fun showVolumeBar(percent: Int) {
        volumeBar.show(percent)
        osdManager.show(OsdManager.OsdState.VOLUME, 2000)
    }

    fun showChannelNumberInput(number: String) {
        channelNumberInput.show(number)
    }

    fun hideChannelNumberInput() {
        channelNumberInput.hide()
    }

    private fun hideSystemUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }
}
