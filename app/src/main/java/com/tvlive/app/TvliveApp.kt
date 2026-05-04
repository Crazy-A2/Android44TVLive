package com.tvlive.app

import android.app.Application
import com.tvlive.app.data.db.AppDatabase
import com.tvlive.app.service.SourceHealthChecker

class TvliveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        SourceHealthChecker().schedule(this)
    }

    companion object {
        lateinit var db: AppDatabase
            private set
    }
}
