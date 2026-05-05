package com.tvlive.app.debug

import android.content.Context
import android.net.wifi.WifiManager
import com.tvlive.app.data.net.HttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.NetworkInterface

object ServerDiscovery {

    private const val PORT = 9753
    private const val PREFS_NAME = "debug_server"
    private const val KEY_SERVER_URL = "server_url"

    fun discover(context: Context): String? {
        // 1. 尝试缓存的地址
        val cached = getCachedUrl(context)
        if (cached != null && ping(cached)) {
            return cached
        }

        // 2. 扫描局域网
        val localIp = getLocalIpAddress() ?: return null
        val prefix = localIp.substringBeforeLast(".")
        for (i in 1..254) {
            val url = "http://$prefix.$i:$PORT"
            if (url != cached && ping(url)) {
                cacheUrl(context, url)
                return url
            }
        }
        return null
    }

    fun ping(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).head().build()
            val response = HttpClient.client.newCall(request).execute()
            response.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getCachedUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, null)
    }

    private fun cacheUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    internal fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in java.util.Collections.list(interfaces)) {
                for (addr in java.util.Collections.list(intf.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
        }
        return null
    }
}
