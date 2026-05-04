package com.tvlive.app.ui.osd

import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

class ChannelNumberInput(private val container: FrameLayout) {

    private val textView = TextView(container.context).apply {
        textSize = 64f
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xCC000000.toInt())
        setPadding(48, 24, 48, 24)
        gravity = Gravity.CENTER
    }

    private val handler = Handler()
    private var hideRunnable: Runnable? = null

    init {
        container.addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
    }

    fun show(number: String) {
        textView.text = number
        container.visibility = View.VISIBLE
        hideRunnable?.let { handler.removeCallbacks(it) }
    }

    fun hide() {
        container.visibility = View.GONE
    }

    fun postHide(delayMs: Long) {
        hideRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { hide() }
        hideRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    fun clear() {
        textView.text = ""
    }
}
