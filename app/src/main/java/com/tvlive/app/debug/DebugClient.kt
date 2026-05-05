package com.tvlive.app.debug

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.tvlive.app.BuildConfig
import com.tvlive.app.data.net.HttpClient
import com.tvlive.app.ui.activity.LivePlayerActivity
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object DebugClient {

    private const val HEARTBEAT_INTERVAL_MS = 30_000L
    private const val COMMAND_POLL_INTERVAL_MS = 5_000L
    private const val DISCOVERY_RETRY_INTERVAL_MS = 30_000L

    private var serverUrl: String? = null
    private var sessionId: String? = null
    private var deviceId: String? = null
    private var activity: LivePlayerActivity? = null
    private var commandExecutor: CommandExecutor? = null
    private var heartbeatThread: Thread? = null
    private var commandThread: Thread? = null
    private var running = false

    private val jsonType = MediaType.parse("application/json; charset=utf-8")!!
    private val sessionFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    fun start(activity: LivePlayerActivity) {
        if (!BuildConfig.DEBUG) return
        if (running) return

        this.activity = activity
        this.commandExecutor = CommandExecutor(activity)
        this.running = true
        this.sessionId = sessionFormat.format(System.currentTimeMillis())
        this.deviceId = generateDeviceId(activity)

        Thread { discoverAndStart(activity) }.start()
    }

    fun stop() {
        running = false
        heartbeatThread?.interrupt()
        commandThread?.interrupt()
        heartbeatThread = null
        commandThread = null
        activity = null
        commandExecutor = null
    }

    fun logError(tag: String, e: Throwable) {
        if (!BuildConfig.DEBUG || serverUrl == null) return
        postLog("ERROR", tag, e.stackTraceToString())
    }

    fun logWarn(tag: String, message: String) {
        if (!BuildConfig.DEBUG || serverUrl == null) return
        postLog("WARN", tag, message)
    }

    fun logInfo(tag: String, message: String) {
        if (!BuildConfig.DEBUG || serverUrl == null) return
        postLog("INFO", tag, message)
    }

    private fun discoverAndStart(context: Context) {
        serverUrl = ServerDiscovery.discover(context)
        if (serverUrl == null) {
            // 未找到服务器，稍后重试
            if (running) {
                try {
                    Thread.sleep(DISCOVERY_RETRY_INTERVAL_MS)
                    if (running) discoverAndStart(context)
                } catch (_: InterruptedException) {}
            }
            return
        }

        // 启动心跳和命令轮询
        heartbeatThread = Thread({ heartbeatLoop() }, "debug-heartbeat").apply { isDaemon = true; start() }
        commandThread = Thread({ commandLoop() }, "debug-command").apply { isDaemon = true; start() }
    }

    private fun heartbeatLoop() {
        while (running) {
            try {
                postStatus()
                Thread.sleep(HEARTBEAT_INTERVAL_MS)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun commandLoop() {
        while (running) {
            try {
                pollCommand()
                Thread.sleep(COMMAND_POLL_INTERVAL_MS)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun postStatus() {
        val url = serverUrl ?: return
        val act = activity ?: return
        try {
            val json = JSONObject().apply {
                put("deviceId", deviceId)
                put("sessionId", sessionId)
                put("appStartTime", sessionId?.replace("_", "T")?.replace("-", ":"))
                put("currentChannel", act.debugGetCurrentChannelName())
                put("playbackState", if (act.debugIsPlaying()) "PLAYING" else "IDLE")
                put("decoderMode", "AUTO")
                put("memoryUsedMB", getUsedMemoryMB(act))
                put("networkType", getNetworkType(act))
                put("deviceInfo", JSONObject().apply {
                    put("model", Build.MODEL)
                    put("androidVersion", Build.VERSION.RELEASE)
                    put("ip", ServerDiscovery.getLocalIpAddress() ?: "")
                    put("appId", act.packageName)
                    put("appVersion", BuildConfig.VERSION_NAME)
                })
            }
            post(url + "/api/status", json.toString())
        } catch (e: Exception) {
        }
    }

    private fun postLog(level: String, tag: String, message: String) {
        val url = serverUrl ?: return
        try {
            val json = JSONObject().apply {
                put("sessionId", sessionId)
                put("level", level)
                put("tag", tag)
                put("message", message)
            }
            post(url + "/api/log", json.toString())
        } catch (e: Exception) {
        }
    }

    private fun pollCommand() {
        val url = serverUrl ?: return
        val did = deviceId ?: return
        try {
            val request = Request.Builder()
                .url("$url/api/command?deviceId=$did")
                .get()
                .build()
            val response = HttpClient.client.newCall(request).execute()
            val body = response.body()?.string()
            response.close()

            if (body != null) {
                val root = JSONObject(body)
                val cmd = root.optJSONObject("command")
                if (cmd != null) {
                    val cmdId = cmd.getString("commandId")
                    val type = cmd.getString("type")
                    val params = cmd.optJSONObject("params") ?: JSONObject()

                    commandExecutor?.execute(type, params)

                    // 确认命令执行
                    val ackJson = JSONObject().put("deviceId", did)
                    post("$url/api/command/$cmdId/ack", ackJson.toString())
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun post(url: String, jsonBody: String) {
        try {
            val body = RequestBody.create(jsonType, jsonBody)
            val request = Request.Builder().url(url).post(body).build()
            val response = HttpClient.client.newCall(request).execute()
            response.close()
        } catch (e: Exception) {
        }
    }

    private fun generateDeviceId(context: Context): String {
        val ip = ServerDiscovery.getLocalIpAddress()
        if (ip != null) {
            val suffix = ip.substringAfterLast(".")
            return "android-tv-$suffix"
        }
        return "android-tv-${Build.SERIAL?.takeLast(6) ?: "unknown"}"
    }

    private fun getUsedMemoryMB(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
        val info = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return (info.totalMem - info.availMem) / (1024 * 1024)
    }

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "UNKNOWN"
        val info = cm.activeNetworkInfo ?: return "UNKNOWN"
        return if (info.type == ConnectivityManager.TYPE_WIFI) "WIFI" else "ETHERNET"
    }
}
