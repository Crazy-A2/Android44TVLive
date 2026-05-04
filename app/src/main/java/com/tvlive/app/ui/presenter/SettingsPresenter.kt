package com.tvlive.app.ui.presenter

import com.tvlive.app.data.db.dao.SourceConfigDao
import com.tvlive.app.data.db.entity.SourceConfig
import com.tvlive.app.player.DecoderMode
import com.tvlive.app.util.PreferenceHelper

class SettingsPresenter(
    private val sourceConfigDao: SourceConfigDao,
    private val prefs: PreferenceHelper
) {

    private var sourceConfigs: List<SourceConfig> = emptyList()

    fun getDecoderModes(): List<DecoderMode> = DecoderMode.values().toList()

    fun getDecoderMode(): DecoderMode {
        return try {
            DecoderMode.valueOf(prefs.decoderMode.toUpperCase())
        } catch (e: Exception) {
            DecoderMode.AUTO
        }
    }

    fun setDecoderMode(mode: DecoderMode) {
        prefs.decoderMode = mode.value
    }

    fun getSourceConfigs(): List<SourceConfig> {
        sourceConfigs = sourceConfigDao.getAll()
        return sourceConfigs
    }

    fun addSourceConfig(name: String, url: String, format: String) {
        sourceConfigDao.insert(SourceConfig(name = name, url = url, format = format, isBuiltin = false))
    }

    fun deleteSourceConfig(id: Long) {
        val config = sourceConfigDao.getById(id) ?: return
        sourceConfigDao.delete(config)
    }

    fun toggleSourceConfig(id: Long) {
        val config = sourceConfigDao.getById(id) ?: return
        config.isEnabled = !config.isEnabled
        sourceConfigDao.update(config)
    }

    fun getAppVersion(): String = "1.0.0"
}
