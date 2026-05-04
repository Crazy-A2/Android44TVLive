package com.tvlive.app.data.parser

object StreamTypeDetector {

    fun detect(url: String): String {
        val lower = url.toLowerCase()
        return when {
            lower.endsWith(".m3u8") || lower.endsWith(".m3u") -> "hls"
            lower.startsWith("rtmp://") -> "rtmp"
            lower.startsWith("rtsp://") -> "rtsp"
            lower.endsWith(".flv") -> "http-flv"
            else -> "other"
        }
    }
}
