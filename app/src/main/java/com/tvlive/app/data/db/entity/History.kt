package com.tvlive.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class History(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "channel_id") var channelId: Long = 0,
    @ColumnInfo(name = "watched_at") var watchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "duration_sec") var durationSec: Long? = null
)
