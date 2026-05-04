package com.tvlive.app.data.db.dao

import android.arch.persistence.room.*
import com.tvlive.app.data.db.entity.Source

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources WHERE channel_id = :channelId AND status != 2 ORDER BY priority ASC LIMIT 1")
    fun getBestSource(channelId: Long): Source?

    @Query("SELECT * FROM sources WHERE channel_id = :channelId AND status != 2 AND id != :excludeSourceId ORDER BY priority ASC")
    fun getNextAvailableSource(channelId: Long, excludeSourceId: Long): Source?

    @Query("SELECT * FROM sources WHERE channel_id = :channelId ORDER BY priority ASC")
    fun getByChannelId(channelId: Long): List<Source>

    @Query("SELECT * FROM sources WHERE status = 2")
    fun getFailedSources(): List<Source>

    @Query("SELECT * FROM sources WHERE source_config_id = :configId")
    fun getBySourceConfigId(configId: Long): List<Source>

    @Query("SELECT DISTINCT c.id FROM channels c " +
            "LEFT JOIN sources s ON c.id = s.channel_id AND s.status != 2 " +
            "WHERE s.id IS NULL AND c.is_visible = 1")
    fun getAllSourcesFailedChannelIds(): List<Long>

    @Query("SELECT COUNT(*) FROM sources WHERE channel_id = :channelId AND status != 2")
    fun countAvailableByChannelId(channelId: Long): Int

    @Query("SELECT COUNT(*) FROM sources WHERE channel_id = :channelId")
    fun countByChannelId(channelId: Long): Int

    @Query("SELECT * FROM sources WHERE channel_id = :channelId AND url = :url LIMIT 1")
    fun getByChannelAndUrl(channelId: Long, url: String): Source?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(source: Source): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(sources: List<Source>): List<Long>

    @Update
    fun update(source: Source)

    @Query("UPDATE sources SET fail_count = fail_count + 1, last_check_time = :now, updated_at = :now WHERE id = :sourceId")
    fun incrementFailCount(sourceId: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE sources SET status = 2, priority = priority + 50, updated_at = :now WHERE id = :sourceId AND fail_count >= 3")
    fun markAsFailed(sourceId: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE sources SET fail_count = 0, status = 0, response_time_ms = :responseTimeMs, last_success_time = :now, last_check_time = :now, updated_at = :now WHERE id = :sourceId")
    fun markAsSuccess(sourceId: Long, responseTimeMs: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE sources SET priority = :newPriority, updated_at = :now WHERE id = :sourceId")
    fun updatePriority(sourceId: Long, newPriority: Int, now: Long = System.currentTimeMillis())

    @Delete
    fun delete(source: Source)

    @Query("DELETE FROM sources WHERE source_config_id = :configId")
    fun deleteBySourceConfigId(configId: Long)

    @Query("DELETE FROM sources WHERE id = :sourceId")
    fun deleteById(sourceId: Long)
}
