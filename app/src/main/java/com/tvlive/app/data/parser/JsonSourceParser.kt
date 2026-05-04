package com.tvlive.app.data.parser

import com.tvlive.app.data.model.ParsedChannel
import com.tvlive.app.data.model.ParsedSource
import com.tvlive.app.data.model.ParseResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Scanner

object JsonSourceParser {

    fun parse(inputStream: InputStream): ParseResult {
        val startTime = System.currentTimeMillis()
        val channels = mutableListOf<ParsedChannel>()

        try {
            val text = Scanner(inputStream, "UTF-8").useDelimiter("\\A").next()
            val root = JSONObject(text)
            val jsonChannels = root.optJSONArray("channels") ?: JSONArray()

            for (i in 0 until jsonChannels.length()) {
                try {
                    val json = jsonChannels.getJSONObject(i)
                    val name = json.optString("name", "").trim()
                    if (name.isEmpty()) continue

                    val jsonSources = json.optJSONArray("sources") ?: JSONArray()
                    val sources = mutableListOf<ParsedSource>()
                    for (j in 0 until jsonSources.length()) {
                        val s = jsonSources.getJSONObject(j)
                        val url = s.optString("url", "").trim()
                        if (url.isEmpty()) continue
                        val type = s.optString("type", "").trim()
                        sources.add(ParsedSource(
                            url = url,
                            streamType = if (type.isNotEmpty()) type else StreamTypeDetector.detect(url),
                            quality = s.optString("quality", null),
                            provider = s.optString("provider", null)
                        ))
                    }
                    if (sources.isEmpty()) continue

                    channels.add(ParsedChannel(
                        name = name,
                        epgId = json.optString("epg_id", null),
                        logoUrl = json.optString("logo", null),
                        category = json.optString("category", "其他"),
                        sources = sources
                    ))
                } catch (e: Exception) {
                    // 单频道解析失败跳过
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParseResult(
            channels = channels,
            parseTimeMs = System.currentTimeMillis() - startTime
        )
    }
}
