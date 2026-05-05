package com.tvlive.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_config")
data class SourceConfig(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "format") var format: String = "m3u",
    @ColumnInfo(name = "is_builtin") var isBuiltin: Boolean = false,
    @ColumnInfo(name = "is_enabled") var isEnabled: Boolean = true,
    @ColumnInfo(name = "last_update_time") var lastUpdateTime: Long? = null,
    @ColumnInfo(name = "update_interval_ms") var updateIntervalMs: Long = 21600000L,
    @ColumnInfo(name = "etag") var etag: String? = null,
    @ColumnInfo(name = "source_priority") var sourcePriority: Int = 100,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
)
