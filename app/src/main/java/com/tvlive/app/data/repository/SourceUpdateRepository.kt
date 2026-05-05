package com.tvlive.app.data.repository

import com.tvlive.app.data.db.dao.ChannelDao
import com.tvlive.app.data.db.dao.SourceConfigDao
import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Source
import com.tvlive.app.data.model.ParseResult
import com.tvlive.app.data.model.ParsedChannel
import com.tvlive.app.data.parser.JsonSourceParser
import com.tvlive.app.data.parser.M3uParser
import java.io.ByteArrayInputStream

class SourceUpdateRepository(
    private val channelDao: ChannelDao,
    private val sourceDao: SourceDao,
    private val sourceConfigDao: SourceConfigDao
) {

    fun parseContent(content: String, format: String): ParseResult? {
        val stream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        return when (format) {
            "m3u", "m3u8" -> M3uParser.parse(stream)
            "json" -> JsonSourceParser.parse(stream)
            else -> null
        }
    }

    fun mergeToDatabase(parsed: ParseResult, configId: Long?, sourcePriority: Int = 100): MergeResult {
        val now = System.currentTimeMillis()
        var inserted = 0
        var updated = 0
        var removed = 0

        val allChannels = channelDao.getAllIncludingHidden()
        var nextChannelNumber = (if (allChannels.isEmpty()) 0 else allChannels.maxBy { it.channelNumber }!!.channelNumber) + 1

        for (pc in parsed.channels) {
            val existing = pc.epgId?.let { channelDao.getByEpgId(it) }
                ?: channelDao.getByNumber(pc.epgId?.filter { it.isDigit() }?.toIntOrNull() ?: -1)

            val channelId: Long
            if (existing != null) {
                // 更新现有频道 metadata
                existing.name = pc.name
                existing.logoUrl = pc.logoUrl
                existing.category = pc.category
                existing.updatedAt = now
                channelDao.update(existing)
                channelId = existing.id
                updated++
            } else {
                // 插入新频道
                val channel = Channel(
                    channelNumber = nextChannelNumber++,
                    name = pc.name,
                    category = pc.category,
                    logoUrl = pc.logoUrl,
                    epgId = pc.epgId
                )
                channelId = channelDao.insert(channel)
                inserted++
            }

            // 插入新源（URL 去重）
            for (ps in pc.sources) {
                val existingSource = sourceDao.getByChannelAndUrl(channelId, ps.url)
                if (existingSource == null) {
                    sourceDao.insert(Source(
                        channelId = channelId,
                        url = ps.url,
                        streamType = ps.streamType,
                        quality = ps.quality,
                        provider = ps.provider,
                        priority = sourcePriority,
                        sourceConfigId = configId
                    ))
                } else {
                    // 更新现有源的 metadata
                    existingSource.streamType = ps.streamType
                    existingSource.quality = ps.quality
                    existingSource.provider = ps.provider
                    existingSource.updatedAt = now
                    sourceDao.update(existingSource)
                }
            }
        }

        // 清理已删除的源（仅针对该配置来源的源）
        if (configId != null) {
            val allNewUrls = parsed.channels.flatMap { pc ->
                pc.sources.map { ps -> ps.url }
            }.toSet()
            val existingSourcesForConfig = sourceDao.getBySourceConfigId(configId)
            for (src in existingSourcesForConfig) {
                if (src.url !in allNewUrls) {
                    sourceDao.delete(src)
                    removed++
                }
            }
        }

        // 更新配置的更新时间和 etag（由调用方处理）

        return MergeResult(inserted, updated, removed)
    }

    fun reorderChannelsByCategory(categoryPriority: List<String>) {
        val allChannels = channelDao.getAllIncludingHidden()
        if (allChannels.isEmpty()) return

        val orderMap = mutableMapOf<String, Int>()
        categoryPriority.forEachIndexed { i, cat -> orderMap[cat] = i }

        val sorted = allChannels.sortedWith(
            compareBy<Channel> { orderMap[it.category] ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )

        var num = 1
        for (ch in sorted) {
            ch.channelNumber = num
            ch.sortOrder = num
            ch.updatedAt = System.currentTimeMillis()
            channelDao.update(ch)
            num++
        }
    }

    data class MergeResult(val inserted: Int, val updated: Int, val removed: Int)
}
