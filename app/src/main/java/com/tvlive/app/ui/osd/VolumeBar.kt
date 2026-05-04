package com.tvlive.app.ui.osd

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.tvlive.app.R

class VolumeBar(private val container: FrameLayout) {

    private val progressBar: ProgressBar
    private val textView: TextView

    init {
        val root = FrameLayout(container.context)
        root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        root.setPadding(80, 0, 80, 40)

        textView = TextView(container.context).apply {
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
        }
        root.addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        progressBar = ProgressBar(container.context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                16,
                Gravity.BOTTOM
            )
        }
        root.addView(progressBar)

        container.addView(root)
    }

    fun show(percent: Int) {
        textView.text = "音量 $percent%"
        progressBar.progress = percent
        container.visibility = View.VISIBLE
    }

    fun hide() {
        container.visibility = View.GONE
    }
}
