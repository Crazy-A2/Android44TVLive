package com.tvlive.app.ui.activity

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tvlive.app.R

class LivePlayerActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var channelInfoBar: FrameLayout
    private lateinit var volumeBarContainer: FrameLayout
    private lateinit var channelNumberInputContainer: FrameLayout
    private lateinit var channelListOverlay: FrameLayout
    private lateinit var settingsOverlay: FrameLayout
    private lateinit var statusMessage: TextView

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_player)

        root = findViewById(R.id.root)
        channelInfoBar = findViewById(R.id.channel_info_bar)
        volumeBarContainer = findViewById(R.id.volume_bar_container)
        channelNumberInputContainer = findViewById(R.id.channel_number_input_container)
        channelListOverlay = findViewById(R.id.channel_list_overlay)
        settingsOverlay = findViewById(R.id.settings_overlay)
        statusMessage = findViewById(R.id.status_message)

        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
