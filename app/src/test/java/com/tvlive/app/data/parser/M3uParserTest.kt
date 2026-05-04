package com.tvlive.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    private fun m3u(content: String) = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    @Test
    fun `parse basic m3u with single channel`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="cctv1" tvg-name="CCTV-1" tvg-logo="http://logo.tv/cctv1.png" group-title="央视",CCTV-1 综合
http://example.com/cctv1.m3u8""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("CCTV-1 综合", result.channels[0].name)
        assertEquals("cctv1", result.channels[0].epgId)
        assertEquals("http://logo.tv/cctv1.png", result.channels[0].logoUrl)
        assertEquals("央视", result.channels[0].category)
        assertEquals(1, result.channels[0].sources.size)
        assertEquals("http://example.com/cctv1.m3u8", result.channels[0].sources[0].url)
        assertEquals("hls", result.channels[0].sources[0].streamType)
    }

    @Test
    fun `parse m3u with multiple channels`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="cctv1" group-title="央视",CCTV-1
http://example.com/cctv1.m3u8
#EXTINF:-1 tvg-id="cctv2" group-title="央视",CCTV-2
http://example.com/cctv2.m3u8""")
        val result = M3uParser.parse(input)
        assertEquals(2, result.channels.size)
        assertEquals("CCTV-1", result.channels[0].name)
        assertEquals("CCTV-2", result.channels[1].name)
    }

    @Test
    fun `parse m3u merges multiple sources for same channel by epgId`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="cctv1",CCTV-1
http://example.com/cctv1_hd.m3u8
http://example.com/cctv1_sd.m3u8""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals(2, result.channels[0].sources.size)
        assertEquals("http://example.com/cctv1_hd.m3u8", result.channels[0].sources[0].url)
        assertEquals("http://example.com/cctv1_sd.m3u8", result.channels[0].sources[1].url)
    }

    @Test
    fun `parse handles rtmp urls`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="hunan",湖南卫视
rtmp://example.com/live/hunan""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("rtmp", result.channels[0].sources[0].streamType)
    }

    @Test
    fun `parse skips empty lines`() {
        val input = m3u("""#EXTM3U

#EXTINF:-1 tvg-id="cctv1",CCTV-1

http://example.com/cctv1.m3u8
""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
    }

    @Test
    fun `parse filters out channels with no sources`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="cctv1",CCTV-1
http://example.com/cctv1.m3u8
#EXTINF:-1 tvg-id="nosource",No Source""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals("cctv1", result.channels[0].epgId)
    }

    @Test
    fun `parse handles gbk encoding fallback`() {
        val input = ByteArrayInputStream("""#EXTM3U
#EXTINF:-1 tvg-id="test",测试
http://example.com/test.m3u8""".toByteArray(Charsets.UTF_8))
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
    }

    @Test
    fun `parse detects stream type for sources`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="multi",Multi
http://example.com/s.m3u8
rtmp://example.com/s
rtsp://example.com/s
http://example.com/s.flv
http://example.com/s.ts""")
        val result = M3uParser.parse(input)
        assertEquals(1, result.channels.size)
        assertEquals(5, result.channels[0].sources.size)
        assertEquals("hls", result.channels[0].sources[0].streamType)
        assertEquals("rtmp", result.channels[0].sources[1].streamType)
        assertEquals("rtsp", result.channels[0].sources[2].streamType)
        assertEquals("http-flv", result.channels[0].sources[3].streamType)
        assertEquals("other", result.channels[0].sources[4].streamType)
    }

    @Test
    fun `parse returns parseTimeMs`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-id="a",A
http://a.m3u8""")
        val result = M3uParser.parse(input)
        assertTrue(result.parseTimeMs >= 0)
    }

    @Test
    fun `parse handles missing tvg-id`() {
        val input = m3u("""#EXTM3U
#EXTINF:-1 tvg-name="CCTV-1",CCTV-1 综合
http://example.com/cctv1.m3u8
#EXTINF:-1 tvg-name="CCTV-2",CCTV-2 财经
http://example.com/cctv2.m3u8""")
        val result = M3uParser.parse(input)
        assertEquals(2, result.channels.size)
    }
}
