package com.tvlive.app.ui.osd

import android.os.Handler

class OsdManager(private val handler: Handler) {

    enum class OsdState { IDLE, CHANNEL_INFO, CHANNEL_LIST, SETTINGS, NUMBER_INPUT, VOLUME }

    var state: OsdState = OsdState.IDLE
        private set

    var onStateChanged: ((OsdState) -> Unit)? = null
        private set

    private var autoHideRunnable: Runnable? = null

    fun show(newState: OsdState, autoHideMs: Long = 0) {
        if (newState == OsdState.IDLE) {
            hide()
            return
        }
        if (state == newState) return
        cancelAutoHide()
        state = newState
        onStateChanged?.invoke(newState)
        if (autoHideMs > 0) {
            val runnable = Runnable { hide() }
            autoHideRunnable = runnable
            handler.postDelayed(runnable, autoHideMs)
        }
    }

    fun hide() {
        if (state == OsdState.IDLE) return
        cancelAutoHide()
        state = OsdState.IDLE
        onStateChanged?.invoke(OsdState.IDLE)
    }

    fun cancelAutoHide() {
        autoHideRunnable?.let { handler.removeCallbacks(it) }
        autoHideRunnable = null
    }
}
