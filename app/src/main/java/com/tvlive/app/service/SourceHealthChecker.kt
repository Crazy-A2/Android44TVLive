package com.tvlive.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tvlive.app.TvliveApp
import com.tvlive.app.data.db.entity.Source
import com.tvlive.app.data.net.HttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SourceHealthChecker : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_CHECK) {
            checkAll(context)
        }
    }

    fun checkAll(context: Context) {
        val db = TvliveApp.db
        val failedSources = db.sourceDao().getFailedSources()
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        val checkClient = HttpClient.client.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        for (source in failedSources) {
            val recovered = probeSource(checkClient, source)
            if (recovered) {
                db.sourceDao().markAsSuccess(source.id, -1, now)
            } else if (shouldDelete(source, now, sevenDaysMs)) {
                db.sourceDao().delete(source)
            }
        }

        // 查询所有源都失效的频道，触发源更新
        val emptyChannelIds = db.sourceDao().getAllSourcesFailedChannelIds()
        if (emptyChannelIds.isNotEmpty()) {
            val updateIntent = Intent(context, SourceUpdateService::class.java)
            updateIntent.action = SourceUpdateService.ACTION_UPDATE_ALL
            context.startService(updateIntent)
        }
    }

    private fun probeSource(client: okhttp3.OkHttpClient, source: Source): Boolean {
        return try {
            val request = Request.Builder().url(source.url).head().build()
            val response = client.newCall(request).execute()
            val success = response.isSuccessful()
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    fun shouldDelete(source: Source, now: Long, sevenDaysMs: Long): Boolean {
        val lastSuccess = source.lastSuccessTime ?: return false
        return (now - lastSuccess > sevenDaysMs) && source.failCount >= 5
    }

    fun schedule(context: Context, intervalMs: Long = 6 * 60 * 60 * 1000L) {
        val intent = Intent(context, SourceHealthChecker::class.java).apply {
            action = ACTION_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            intervalMs,
            intervalMs,
            pendingIntent
        )
    }

    companion object {
        const val ACTION_CHECK = "com.tvlive.app.action.HEALTH_CHECK"
    }
}
