package com.tvlive.app.ui.osd

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tvlive.app.R

class ChannelInfoBar(private val container: FrameLayout) {

    private val numberText: TextView = container.findViewById(R.id.channel_number)
    private val nameText: TextView = container.findViewById(R.id.channel_name)

    fun show(channelNumber: Int, channelName: String) {
        numberText.text = channelNumber.toString()
        nameText.text = channelName
        container.visibility = View.VISIBLE
    }

    fun hide() {
        container.visibility = View.GONE
    }
}
