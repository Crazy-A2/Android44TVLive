package com.tvlive.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.tvlive.app.data.db.dao.*
import com.tvlive.app.data.db.entity.*

@Database(
    entities = [
        Channel::class,
        Source::class,
        Favorite::class,
        History::class,
        SourceConfig::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao
    abstract fun sourceDao(): SourceDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun sourceConfigDao(): SourceConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tvlive.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
