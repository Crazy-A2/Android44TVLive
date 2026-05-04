package com.tvlive.app.data.db.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "channel_number") var channelNumber: Int = 0,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "category") var category: String = "其他",
    @ColumnInfo(name = "logo_url") var logoUrl: String? = null,
    @ColumnInfo(name = "epg_id") var epgId: String? = null,
    @ColumnInfo(name = "sort_order") var sortOrder: Int = 0,
    @ColumnInfo(name = "is_visible") var isVisible: Boolean = true,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
)
