package com.tvlive.app.data.parser

import com.tvlive.app.data.model.ParsedChannel
import com.tvlive.app.data.model.ParsedSource
import com.tvlive.app.data.model.ParseResult
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

object M3uParser {

    fun parse(inputStream: InputStream): ParseResult {
        val startTime = System.currentTimeMillis()

        val channels = mutableMapOf<String, ParsedChannel>()
        val channelOrder = mutableListOf<String>()
        var currentEpgId: String? = null
        var currentName: String? = null
        var currentLogo: String? = null
        var currentCategory: String? = null

        fun pushCurrent() {
            val epgId = currentEpgId ?: currentName?.replace(" ", "")
            val name = currentName ?: return
            if (epgId == null) return

            val key = epgId
            if (!channels.containsKey(key)) {
                channels[key] = ParsedChannel(
                    name = name,
                    epgId = epgId,
                    logoUrl = currentLogo,
                    category = currentCategory ?: "其他",
                    sources = emptyList()
                )
                channelOrder.add(key)
            }
            currentEpgId = null
            currentName = null
            currentLogo = null
            currentCategory = null
        }

        fun addSourceToChannel(epgId: String?, url: String) {
            val key = epgId ?: return
            if (!channels.containsKey(key)) {
                val name = currentName ?: return
                channels[key] = ParsedChannel(
                    name = name,
                    epgId = key,
                    logoUrl = currentLogo,
                    category = currentCategory ?: "其他",
                    sources = emptyList()
                )
                channelOrder.add(key)
            }
            val existing = channels[key] ?: return
            val source = ParsedSource(
                url = url,
                streamType = StreamTypeDetector.detect(url),
                quality = null,
                provider = null
            )
            channels[key] = existing.copy(sources = existing.sources + source)
        }

        val reader = try {
            BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        } catch (e: Exception) {
            BufferedReader(InputStreamReader(inputStream, Charset.forName("GBK")))
        }

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTM3U", ignoreCase = true)) continue

                if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                    pushCurrent()

                    val tvgIdMatch = Regex("""tvg-id="([^"]*)"""").find(trimmed)
                    val tvgNameMatch = Regex("""tvg-name="([^"]*)"""").find(trimmed)
                    val tvgLogoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmed)
                    val groupTitleMatch = Regex("""group-title="([^"]*)"""").find(trimmed)
                    val commaNameMatch = Regex(""",(.+)$""").find(trimmed)

                    currentEpgId = tvgIdMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                    currentName = commaNameMatch?.groupValues?.get(1)?.trim()
                        ?: tvgNameMatch?.groupValues?.get(1)?.trim()
                    currentLogo = tvgLogoMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                    currentCategory = groupTitleMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                } else if (!trimmed.startsWith("#")) {
                    addSourceToChannel(currentEpgId ?: currentName?.replace(" ", ""), trimmed)
                }
            }
        }
        pushCurrent()

        val result = channelOrder.mapNotNull { channels[it] }
            .filter { it.sources.isNotEmpty() }

        return ParseResult(
            channels = result,
            parseTimeMs = System.currentTimeMillis() - startTime
        )
    }
}
