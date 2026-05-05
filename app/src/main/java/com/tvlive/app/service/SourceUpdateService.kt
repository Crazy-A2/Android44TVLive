package com.tvlive.app.service

import android.app.IntentService
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tvlive.app.TvliveApp
import com.tvlive.app.data.db.entity.SourceConfig
import com.tvlive.app.data.net.SourceFetcher
import com.tvlive.app.data.repository.SourceUpdateRepository
import com.tvlive.app.util.PreferenceHelper

class SourceUpdateService : IntentService("SourceUpdateService") {

    private val repo by lazy {
        val db = TvliveApp.db
        SourceUpdateRepository(db.channelDao(), db.sourceDao(), db.sourceConfigDao())
    }

    private val prefs by lazy { PreferenceHelper(this) }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_UPDATE_ALL -> updateAll()
            ACTION_UPDATE_CONFIG -> {
                val configId = intent.getLongExtra(EXTRA_CONFIG_ID, -1L)
                if (configId != -1L) {
                    updateConfig(configId)
                }
            }
            ACTION_UPDATE_CHANNEL -> {
                val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, -1L)
                if (channelId != -1L) {
                    updateSingleChannel(channelId)
                }
            }
        }
    }

    private fun updateAll() {
        val configs = TvliveApp.db.sourceConfigDao().getEnabled()
        for (config in configs) {
            updateSingleConfig(config)
        }
        if (!prefs.channelsOrdered) {
            repo.reorderChannelsByCategory(
                prefs.categoryPriority.split(",").map { it.trim() }
            )
            if (TvliveApp.db.channelDao().count() > 0) {
                prefs.channelsOrdered = true
            }
        }
        broadcastUpdateComplete(null)
    }

    private fun updateConfig(configId: Long) {
        val config = TvliveApp.db.sourceConfigDao().getById(configId) ?: return
        updateSingleConfig(config)
        broadcastUpdateComplete(configId)
    }

    private fun updateSingleConfig(config: SourceConfig) {
        try {
            val result = SourceFetcher.fetch(config.url, config.etag)
            if (!result.isModified || result.content == null) return

            val parsed = repo.parseContent(result.content, config.format) ?: return
            repo.mergeToDatabase(parsed, config.id, config.sourcePriority)

            config.etag = result.etag
            config.lastUpdateTime = System.currentTimeMillis()
            TvliveApp.db.sourceConfigDao().update(config)
        } catch (e: Exception) {
            com.tvlive.app.debug.DebugClient.logError("SourceUpdateService", e)
        }
    }

    private fun updateSingleChannel(channelId: Long) {
        // 触发源更新后尝试重新获取该频道的最佳源
        val configs = TvliveApp.db.sourceConfigDao().getEnabled()
        for (config in configs) {
            updateSingleConfig(config)
        }
        broadcastUpdateComplete(null)
    }

    private fun broadcastUpdateComplete(configId: Long?) {
        val intent = Intent(BROADCAST_UPDATE_COMPLETE).apply {
            if (configId != null) {
                putExtra(EXTRA_CONFIG_ID, configId)
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val ACTION_UPDATE_ALL = "com.tvlive.app.action.UPDATE_ALL"
        const val ACTION_UPDATE_CONFIG = "com.tvlive.app.action.UPDATE_CONFIG"
        const val ACTION_UPDATE_CHANNEL = "com.tvlive.app.action.UPDATE_CHANNEL"
        const val EXTRA_CONFIG_ID = "config_id"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val BROADCAST_UPDATE_COMPLETE = "com.tvlive.app.UPDATE_COMPLETE"
    }
}
