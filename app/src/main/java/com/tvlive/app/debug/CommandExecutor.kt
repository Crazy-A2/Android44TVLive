package com.tvlive.app.debug

import android.content.Intent
import com.tvlive.app.player.DecoderMode
import com.tvlive.app.service.SourceUpdateService
import com.tvlive.app.ui.activity.LivePlayerActivity
import org.json.JSONObject

class CommandExecutor(private val activity: LivePlayerActivity) {

    fun execute(type: String, params: JSONObject) {
        when (type) {
            "RELOAD_SOURCES" -> reloadSources()
            "SWITCH_CHANNEL" -> switchChannel(params)
            "SWITCH_DECODER" -> switchDecoder(params)
            "GET_LOGCAT" -> getLogcat()
            "RESTART_APP" -> restartApp()
        }
    }

    private fun reloadSources() {
        val intent = Intent(activity, SourceUpdateService::class.java)
        intent.action = SourceUpdateService.ACTION_UPDATE_ALL
        activity.startService(intent)
    }

    private fun switchChannel(params: JSONObject) {
        val channelNumber = params.optInt("channelNumber", -1)
        if (channelNumber > 0) {
            activity.debugSwitchChannel(channelNumber)
        }
    }

    private fun switchDecoder(params: JSONObject) {
        val mode = params.optString("mode", "AUTO")
        activity.debugSetDecoderMode(
            when (mode) {
                "SOFTWARE" -> DecoderMode.SOFTWARE
                "HARDWARE" -> DecoderMode.HARDWARE
                else -> DecoderMode.AUTO
            }
        )
    }

    private fun getLogcat() {
        try {
            val process = Runtime.getRuntime().exec("logcat -d -t 200")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            DebugClient.logInfo("Logcat", output.take(5000))
        } catch (e: Exception) {
            DebugClient.logError("Logcat", e)
        }
    }

    private fun restartApp() {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
            activity.finish()
            Runtime.getRuntime().exit(0)
        }
    }
}
