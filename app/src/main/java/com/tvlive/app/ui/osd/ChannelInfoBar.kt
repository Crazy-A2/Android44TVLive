package com.tvlive.app.ui.osd

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.tvlive.app.R

class ChannelInfoBar(private val container: FrameLayout) {

    private val logoImage: ImageView = container.findViewById(R.id.channel_logo)
    private val numberText: TextView = container.findViewById(R.id.channel_number)
    private val nameText: TextView = container.findViewById(R.id.channel_name)
    private val epgText: TextView = container.findViewById(R.id.channel_epg)

    fun show(channelNumber: Int, channelName: String, logoUrl: String? = null, epgTitle: String? = null) {
        numberText.text = channelNumber.toString()
        nameText.text = channelName

        if (!logoUrl.isNullOrEmpty()) {
            logoImage.visibility = View.VISIBLE
            Glide.with(logoImage.context)
                .load(logoUrl)
                .circleCrop()
                .into(logoImage)
        } else {
            logoImage.visibility = View.GONE
        }

        if (!epgTitle.isNullOrEmpty()) {
            epgText.text = epgTitle
            epgText.visibility = View.VISIBLE
        } else {
            epgText.visibility = View.GONE
        }

        container.visibility = View.VISIBLE
    }

    fun hide() {
        container.visibility = View.GONE
    }
}
