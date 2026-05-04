package com.tvlive.app.data.parser

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class JsonSourceParserTest {

    private fun json(content: String) = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    @Test
    fun `parse basic json with single channel`() {
        val input = json("""
        {"channels":[{"name":"CCTV-1","epg_id":"cctv1","logo":"http://logo.tv/cctv1.png","category":"央视","sources":[{"url":"http://example.com/cctv1.m3u8","type":"hls","quality":"hd","provider":"主源"}]}]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("CCTV-1", result.channels[0].name)
        assertEquals("cctv1", result.channels[0].epgId)
        assertEquals("http://logo.tv/cctv1.png", result.channels[0].logoUrl)
        assertEquals("央视", result.channels[0].category)
        assertEquals(1, result.channels[0].sources.size)
        assertEquals("http://example.com/cctv1.m3u8", result.channels[0].sources[0].url)
        assertEquals("hls", result.channels[0].sources[0].streamType)
        assertEquals("hd", result.channels[0].sources[0].quality)
        assertEquals("主源", result.channels[0].sources[0].provider)
    }

    @Test
    fun `parse multiple channels`() {
        val input = json("""
        {"channels":[
            {"name":"CCTV-1","epg_id":"cctv1","category":"央视","sources":[{"url":"http://a.m3u8","type":"hls"}]},
            {"name":"CCTV-2","epg_id":"cctv2","category":"央视","sources":[{"url":"http://b.m3u8","type":"hls"}]}
        ]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(2, result.channels.size)
    }

    @Test
    fun `parse channel with multiple sources`() {
        val input = json("""
        {"channels":[{"name":"CCTV-1","epg_id":"cctv1","category":"央视","sources":[
            {"url":"http://hd.m3u8","type":"hls","quality":"hd"},
            {"url":"http://sd.m3u8","type":"hls","quality":"sd"}
        ]}]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals(2, result.channels[0].sources.size)
        assertEquals("hd", result.channels[0].sources[0].quality)
        assertEquals("sd", result.channels[0].sources[1].quality)
    }

    @Test
    fun `parse infers type from url when not provided`() {
        val input = json("""
        {"channels":[{"name":"Test","epg_id":"test","sources":[
            {"url":"http://test.m3u8"},
            {"url":"rtmp://test/stream"}
        ]}]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals("hls", result.channels[0].sources[0].streamType)
        assertEquals("rtmp", result.channels[0].sources[1].streamType)
    }

    @Test
    fun `parse handles missing optional fields`() {
        val input = json("""
        {"channels":[{"name":"CCTV-1","sources":[{"url":"http://a.m3u8","type":"hls"}]}]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(1, result.channels.size)
        assertNull(result.channels[0].epgId)
        assertNull(result.channels[0].logoUrl)
        assertEquals("其他", result.channels[0].category)
    }

    @Test
    fun `parse skips channel with no sources`() {
        val input = json("""
        {"channels":[
            {"name":"A","epg_id":"a","sources":[{"url":"http://a.m3u8","type":"hls"}]},
            {"name":"B","epg_id":"b","sources":[]}
        ]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("A", result.channels[0].name)
    }

    @Test
    fun `parse skips channel missing name`() {
        val input = json("""
        {"channels":[
            {"epg_id":"a","sources":[{"url":"http://a.m3u8","type":"hls"}]},
            {"name":"B","sources":[{"url":"http://b.m3u8","type":"hls"}]}
        ]}
        """.trimIndent())
        val result = JsonSourceParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("B", result.channels[0].name)
    }

    @Test
    fun `parse handles empty channels array`() {
        val input = json("""{"channels":[]}""".trimIndent())
        val result = JsonSourceParser.parse(input)
        assertTrue(result.channels.isEmpty())
    }

    @Test
    fun `parse returns parseTimeMs`() {
        val input = json("""{"channels":[{"name":"A","sources":[{"url":"http://a.m3u8","type":"hls"}]}]}""")
        val result = JsonSourceParser.parse(input)
        assertTrue(result.parseTimeMs >= 0)
    }
}
