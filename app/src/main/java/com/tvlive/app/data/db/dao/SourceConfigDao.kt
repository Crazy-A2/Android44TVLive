package com.tvlive.app.data.db.dao

import android.arch.persistence.room.*
import com.tvlive.app.data.db.entity.SourceConfig

@Dao
interface SourceConfigDao {

    @Query("SELECT * FROM source_config ORDER BY is_builtin DESC, created_at ASC")
    fun getAll(): List<SourceConfig>

    @Query("SELECT * FROM source_config WHERE is_enabled = 1")
    fun getEnabled(): List<SourceConfig>

    @Query("SELECT * FROM source_config WHERE id = :id LIMIT 1")
    fun getById(id: Long): SourceConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: SourceConfig): Long

    @Update
    fun update(config: SourceConfig)

    @Delete
    fun delete(config: SourceConfig)
}
