package com.tvlive.app.data.parser

object StreamTypeDetector {

    fun detect(url: String): String {
        val clean = url.split("?", limit = 2)[0].toLowerCase()
        return when {
            clean.endsWith(".m3u8") || clean.endsWith(".m3u") -> "hls"
            url.startsWith("rtmp://", ignoreCase = true) -> "rtmp"
            url.startsWith("rtsp://", ignoreCase = true) -> "rtsp"
            clean.endsWith(".flv") -> "http-flv"
            else -> "other"
        }
    }
}
