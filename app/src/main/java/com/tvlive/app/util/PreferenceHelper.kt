package com.tvlive.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tvlive_prefs", Context.MODE_PRIVATE)

    var lastChannelId: Long
        get() = prefs.getLong("last_channel_id", -1L)
        set(value) = prefs.edit().putLong("last_channel_id", value).apply()

    var volumeLevel: Int
        get() = prefs.getInt("volume_level", 80)
        set(value) = prefs.edit().putInt("volume_level", value.coerceIn(0, 100)).apply()

    var decoderMode: String
        get() = prefs.getString("decoder_mode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("decoder_mode", value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(value) = prefs.edit().putBoolean("first_launch", value).apply()

    var categoryPriority: String
        get() = prefs.getString("category_priority", "央视,卫视,付费") ?: "央视,卫视,付费"
        set(value) = prefs.edit().putString("category_priority", value).apply()

    var channelsOrdered: Boolean
        get() = prefs.getBoolean("channels_ordered", false)
        set(value) = prefs.edit().putBoolean("channels_ordered", value).apply()
}
