package com.tvlive.app.data.db.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

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
