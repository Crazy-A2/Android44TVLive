package com.tvlive.app.data.db.dao

import android.arch.persistence.room.*
import com.tvlive.app.data.db.entity.History

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY watched_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): List<History>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(history: History): Long

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY watched_at DESC LIMIT :keepCount)")
    fun trimTo(keepCount: Int = 100)

    @Query("DELETE FROM history")
    fun deleteAll()
}
