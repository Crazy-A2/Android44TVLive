package com.tvlive.app.data.repository

import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Source

class SourceRepository(private val sourceDao: SourceDao) {

    fun reportSourceFailed(sourceId: Long): Source? {
        val now = System.currentTimeMillis()
        sourceDao.incrementFailCount(sourceId, now)
        val source = sourceDao.getById(sourceId) ?: return null
        if (source.failCount >= 3) {
            sourceDao.markAsFailed(sourceId, now)
        }
        return source
    }

    fun reportSourceSuccess(sourceId: Long, responseTimeMs: Int) {
        val now = System.currentTimeMillis()
        sourceDao.markAsSuccess(sourceId, responseTimeMs, now)
        val source = sourceDao.getById(sourceId) ?: return
        var newPriority = source.priority
        if (responseTimeMs < 500) {
            newPriority -= 15
        } else if (responseTimeMs > 3000) {
            newPriority += 10
        } else {
            return
        }
        newPriority = newPriority.coerceAtLeast(1)
        if (newPriority != source.priority) {
            sourceDao.updatePriority(sourceId, newPriority, now)
        }
    }

    fun getNextAvailableSource(channelId: Long, excludeSourceId: Long): Source? {
        return sourceDao.getNextAvailableSource(channelId, excludeSourceId)
    }

    fun getBestSource(channelId: Long): Source? {
        return sourceDao.getBestSource(channelId)
    }

    fun hasAvailableSource(channelId: Long): Boolean {
        return sourceDao.countAvailableByChannelId(channelId) > 0
    }
}
