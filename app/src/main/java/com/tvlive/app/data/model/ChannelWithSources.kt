package com.tvlive.app.data.model

import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Source

data class ChannelWithSources(
    val channel: Channel,
    val sources: List<Source>
)
