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
import com.tvlive.app.util.PreferenceHelper

class LivePlayerActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var channelInfoBar: FrameLayout
    private lateinit var volumeBarContainer: FrameLayout
    private lateinit var channelNumberInputContainer: FrameLayout
    private lateinit var channelListOverlay: FrameLayout
    private lateinit var settingsOverlay: FrameLayout
    private lateinit var statusMessage: TextView

    private lateinit var playerManager: PlayerManager
    private lateinit var prefs: PreferenceHelper
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_player)

        root = findViewById(R.id.root)
        surfaceView = findViewById(R.id.surface_view)
        channelInfoBar = findViewById(R.id.channel_info_bar)
        volumeBarContainer = findViewById(R.id.volume_bar_container)
        channelNumberInputContainer = findViewById(R.id.channel_number_input_container)
        channelListOverlay = findViewById(R.id.channel_list_overlay)
        settingsOverlay = findViewById(R.id.settings_overlay)
        statusMessage = findViewById(R.id.status_message)

        prefs = PreferenceHelper(this)
        initPlayer()
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
        // 后续步骤中实现按键分发逻辑
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        // 覆盖层打开时关闭覆盖层，而非退出
        if (channelListOverlay.visibility == View.VISIBLE) {
            channelListOverlay.visibility = View.GONE
            return
        }
        if (settingsOverlay.visibility == View.VISIBLE) {
            settingsOverlay.visibility = View.GONE
            return
        }
    }

    private fun initPlayer() {
        playerManager = PlayerManager()
        playerManager.init(surfaceView)
        playerManager.setCallback(object : PlayerCallback {
            override fun onPrepared() {
                hideStatusMessage()
            }

            override fun onError(what: Int, extra: Int) {
                showStatusMessage("播放失败")
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

    private fun showStatusMessage(text: String) {
        statusMessage.text = text
        statusMessage.visibility = View.VISIBLE
    }

    private fun hideStatusMessage() {
        statusMessage.visibility = View.GONE
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
