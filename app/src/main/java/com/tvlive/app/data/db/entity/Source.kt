package com.tvlive.app.data.db.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceConfig::class,
            parentColumns = ["id"],
            childColumns = ["source_config_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["channel_id", "priority"]),
        Index(value = ["source_config_id"])
    ]
)
data class Source(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "channel_id") var channelId: Long = 0,
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "stream_type") var streamType: String = "hls",
    @ColumnInfo(name = "quality") var quality: String? = null,
    @ColumnInfo(name = "provider") var provider: String? = null,
    @ColumnInfo(name = "priority") var priority: Int = 100,
    @ColumnInfo(name = "status") var status: Int = STATUS_NORMAL,
    @ColumnInfo(name = "fail_count") var failCount: Int = 0,
    @ColumnInfo(name = "last_check_time") var lastCheckTime: Long? = null,
    @ColumnInfo(name = "last_success_time") var lastSuccessTime: Long? = null,
    @ColumnInfo(name = "response_time_ms") var responseTimeMs: Int? = null,
    @ColumnInfo(name = "source_config_id") var sourceConfigId: Long? = null,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_NORMAL = 0
        const val STATUS_SLOW = 1
        const val STATUS_FAILED = 2
        const val STATUS_CHECKING = 3
    }
}
