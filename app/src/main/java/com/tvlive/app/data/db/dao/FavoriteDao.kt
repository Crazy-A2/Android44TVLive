package com.tvlive.app.data.db.dao

import androidx.room.*
import com.tvlive.app.data.db.entity.Favorite

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY created_at DESC")
    fun getAll(): List<Favorite>

    @Query("SELECT * FROM favorites WHERE channel_id = :channelId LIMIT 1")
    fun getByChannelId(channelId: Long): Favorite?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channel_id = :channelId)")
    fun isFavorite(channelId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favorite: Favorite): Long

    @Delete
    fun delete(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE channel_id = :channelId")
    fun deleteByChannelId(channelId: Long)
}
