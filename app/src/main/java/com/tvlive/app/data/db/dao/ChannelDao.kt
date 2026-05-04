package com.tvlive.app.data.db.dao

import android.arch.persistence.room.*
import com.tvlive.app.data.db.entity.Channel

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE is_visible = 1 ORDER BY sort_order ASC, channel_number ASC")
    fun getAllOrdered(): List<Channel>

    @Query("SELECT * FROM channels ORDER BY sort_order ASC, channel_number ASC")
    fun getAllIncludingHidden(): List<Channel>

    @Query("SELECT * FROM channels WHERE channel_number = :number LIMIT 1")
    fun getByNumber(number: Int): Channel?

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    fun getById(id: Long): Channel?

    @Query("SELECT * FROM channels WHERE epg_id = :epgId LIMIT 1")
    fun getByEpgId(epgId: String): Channel?

    @Query("SELECT * FROM channels WHERE category = :category AND is_visible = 1 ORDER BY sort_order ASC, channel_number ASC")
    fun getByCategory(category: String): List<Channel>

    @Query("SELECT COUNT(*) FROM channels")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(channel: Channel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(channels: List<Channel>): List<Long>

    @Update
    fun update(channel: Channel)

    @Delete
    fun delete(channel: Channel)
}
