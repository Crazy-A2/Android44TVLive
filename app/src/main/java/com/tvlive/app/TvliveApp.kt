package com.tvlive.app

import android.app.Application
import com.tvlive.app.data.db.AppDatabase

class TvliveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var db: AppDatabase
            private set
    }
}
