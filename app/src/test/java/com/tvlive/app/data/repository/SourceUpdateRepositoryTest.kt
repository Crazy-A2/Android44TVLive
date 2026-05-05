package com.tvlive.app.data.repository

import com.tvlive.app.data.db.dao.ChannelDao
import com.tvlive.app.data.db.dao.SourceConfigDao
import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Source
import com.tvlive.app.data.model.ParsedChannel
import com.tvlive.app.data.model.ParsedSource
import com.tvlive.app.data.model.ParseResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyLong

class SourceUpdateRepositoryTest {

    private lateinit var channelDao: ChannelDao
    private lateinit var sourceDao: SourceDao
    private lateinit var sourceConfigDao: SourceConfigDao
    private lateinit var repo: SourceUpdateRepository

    @Before
    fun setUp() {
        channelDao = mock(ChannelDao::class.java)
        sourceDao = mock(SourceDao::class.java)
        sourceConfigDao = mock(SourceConfigDao::class.java)
        repo = SourceUpdateRepository(channelDao, sourceDao, sourceConfigDao)
    }

    @Test
    fun `mergeToDatabase inserts new channel and sources`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(emptyList())
        `when`(channelDao.insert((any(Channel::class.java) as? Channel) ?: Channel())).thenReturn(1L)

        val parsed = ParseResult(listOf(ParsedChannel(
            name = "CCTV-1", epgId = "cctv1", category = "央视", logoUrl = null,
            sources = listOf(ParsedSource("http://a.m3u8", "hls", null, null))
        )), 0)

        repo.mergeToDatabase(parsed, null)
        verify(channelDao).insert((any(Channel::class.java) as? Channel) ?: Channel())
        verify(sourceDao).insert((any(Source::class.java) as? Source) ?: Source())
    }

    @Test
    fun `mergeToDatabase updates existing channel matched by epgId`() {
        `when`(channelDao.getByEpgId("cctv1")).thenReturn(Channel(id = 1, channelNumber = 1, name = "旧名"))
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(Channel(id = 1, channelNumber = 1, name = "旧名")))

        val parsed = ParseResult(listOf(ParsedChannel(
            name = "CCTV-1", epgId = "cctv1", category = "央视", logoUrl = null,
            sources = listOf(ParsedSource("http://a.m3u8", "hls", null, null))
        )), 0)

        repo.mergeToDatabase(parsed, null)
        verify(channelDao).update((any(Channel::class.java) as? Channel) ?: Channel())
    }

    @Test
    fun `mergeToDatabase deduplicates sources by url`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(emptyList())
        `when`(channelDao.insert((any(Channel::class.java) as? Channel) ?: Channel())).thenReturn(1L)
        `when`(sourceDao.getByChannelAndUrl(1L, "http://a.m3u8")).thenReturn(
            Source(id = 1, channelId = 1, url = "http://a.m3u8")
        )

        val parsed = ParseResult(listOf(ParsedChannel(
            name = "CCTV-1", epgId = "cctv1", category = "央视", logoUrl = null,
            sources = listOf(ParsedSource("http://a.m3u8", "hls", null, null))
        )), 0)

        repo.mergeToDatabase(parsed, null)
        // URL 已存在，不插入新源
        verify(sourceDao, never()).insert((any(Source::class.java) as? Source) ?: Source())
        // 但更新 metadata
        verify(sourceDao).update((any(Source::class.java) as? Source) ?: Source())
    }

    @Test
    fun `mergeToDatabase removes old sources for specific config`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(emptyList())
        `when`(channelDao.insert((any(Channel::class.java) as? Channel) ?: Channel())).thenReturn(1L)
        `when`(sourceDao.getBySourceConfigId(1L)).thenReturn(listOf(
            Source(id = 2, channelId = 1, url = "http://old.m3u8", sourceConfigId = 1)
        ))

        val parsed = ParseResult(listOf(ParsedChannel(
            name = "CCTV-1", epgId = "cctv1", category = "央视", logoUrl = null,
            sources = listOf(ParsedSource("http://new.m3u8", "hls", null, null))
        )), 0)

        repo.mergeToDatabase(parsed, 1L)
        // 新 URL "new" 插入
        verify(sourceDao).insert((any(Source::class.java) as? Source) ?: Source())
        // 旧 URL "old" 删除
        verify(sourceDao).delete((any(Source::class.java) as? Source) ?: Source())
    }

    @Test
    fun `parseContent parses m3u format`() {
        val content = "#EXTM3U\n#EXTINF:-1 tvg-id=\"cctv1\",CCTV-1\nhttp://a.m3u8"
        val result = repo.parseContent(content, "m3u")
        assertNotNull(result)
        assertEquals(1, result!!.channels.size)
    }

    @Test
    fun `parseContent parses json format`() {
        val content = """{"channels":[{"name":"CCTV-1","sources":[{"url":"http://a.m3u8","type":"hls"}]}]}"""
        val result = repo.parseContent(content, "json")
        assertNotNull(result)
        assertEquals(1, result!!.channels.size)
    }

    @Test
    fun `parseContent returns null for unknown format`() {
        val result = repo.parseContent("content", "txt")
        assertNull(result)
    }
}
