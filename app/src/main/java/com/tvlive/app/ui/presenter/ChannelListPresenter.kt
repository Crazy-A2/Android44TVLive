package com.tvlive.app.ui.presenter

import com.tvlive.app.data.db.dao.ChannelDao
import com.tvlive.app.data.db.dao.FavoriteDao
import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Favorite

class ChannelListPresenter(
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val sourceDao: SourceDao
) {

    private var allChannels: List<Channel> = emptyList()
    private var currentChannels: List<Channel> = emptyList()
    private var favoriteIds: Set<Long> = emptySet()
    private var currentCategory: String = "全部"

    fun init() {
        allChannels = channelDao.getAllIncludingHidden()
        favoriteIds = favoriteDao.getAll().map { it.channelId }.toSet()
        currentChannels = allChannels
    }

    fun getCategories(): List<String> {
        val categories = allChannels.map { it.category }.distinct().sorted()
        return listOf("全部") + categories + listOf("收藏")
    }

    fun selectCategory(category: String) {
        currentCategory = category
        currentChannels = when (category) {
            "全部" -> allChannels
            "收藏" -> allChannels.filter { it.id in favoriteIds }
            else -> allChannels.filter { it.category == category }
        }
    }

    fun getCurrentCategory(): String = currentCategory

    fun getCurrentChannels(): List<Channel> = currentChannels

    fun getAllChannels(): List<Channel> = allChannels

    fun getCurrentChannelIndex(channelId: Long): Int =
        currentChannels.indexOfFirst { it.id == channelId }

    fun toggleFavorite(channelId: Long) {
        if (favoriteDao.isFavorite(channelId)) {
            favoriteDao.deleteByChannelId(channelId)
            favoriteIds = favoriteIds - channelId
        } else {
            favoriteDao.insert(Favorite(channelId = channelId))
            favoriteIds = favoriteIds + channelId
        }
        if (currentCategory == "收藏") {
            currentChannels = allChannels.filter { it.id in favoriteIds }
        }
    }

    fun isFavorite(channelId: Long): Boolean = channelId in favoriteIds
}
