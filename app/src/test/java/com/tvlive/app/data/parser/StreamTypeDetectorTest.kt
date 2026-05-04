package com.tvlive.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamTypeDetectorTest {

    @Test
    fun `detect returns hls for m3u8 url`() {
        assertEquals("hls", StreamTypeDetector.detect("http://example.com/stream.m3u8"))
    }

    @Test
    fun `detect returns hls for m3u url`() {
        assertEquals("hls", StreamTypeDetector.detect("http://example.com/stream.m3u"))
    }

    @Test
    fun `detect returns rtmp for rtmp url`() {
        assertEquals("rtmp", StreamTypeDetector.detect("rtmp://example.com/live/stream"))
    }

    @Test
    fun `detect returns rtsp for rtsp url`() {
        assertEquals("rtsp", StreamTypeDetector.detect("rtsp://example.com/stream"))
    }

    @Test
    fun `detect returns http-flv for flv url`() {
        assertEquals("http-flv", StreamTypeDetector.detect("http://example.com/stream.flv"))
    }

    @Test
    fun `detect returns other for unknown format`() {
        assertEquals("other", StreamTypeDetector.detect("http://example.com/stream.ts"))
    }

    @Test
    fun `detect is case insensitive`() {
        assertEquals("hls", StreamTypeDetector.detect("http://example.com/stream.M3U8"))
        assertEquals("hls", StreamTypeDetector.detect("http://example.com/stream.M3U"))
    }

    @Test
    fun `detect handles https`() {
        assertEquals("hls", StreamTypeDetector.detect("https://example.com/stream.m3u8"))
    }

    @Test
    fun `detect handles url with query string`() {
        assertEquals("hls", StreamTypeDetector.detect("http://example.com/stream.m3u8?token=abc&exp=123"))
    }

    @Test
    fun `detect returns other for empty string`() {
        assertEquals("other", StreamTypeDetector.detect(""))
    }
}
