package com.tvlive.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Favorite(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "channel_id") var channelId: Long = 0,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
)
