package com.tvlive.app.data.repository

import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Source
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.*

class SourceRepositoryTest {

    private lateinit var sourceDao: SourceDao
    private lateinit var repo: SourceRepository

    @Before
    fun setUp() {
        sourceDao = mock(SourceDao::class.java)
        repo = SourceRepository(sourceDao)
    }

    @Test
    fun `reportSourceFailed increments failCount`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, failCount = 1))
        repo.reportSourceFailed(1L)
        verify(sourceDao).incrementFailCount(eq(1L), anyLong())
    }

    @Test
    fun `reportSourceFailed marks as failed when count reaches threshold`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, failCount = 3))
        repo.reportSourceFailed(1L)
        verify(sourceDao).markAsFailed(eq(1L), anyLong())
    }

    @Test
    fun `reportSourceFailed does not mark when count below threshold`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, failCount = 1))
        repo.reportSourceFailed(1L)
        verify(sourceDao, never()).markAsFailed(anyLong(), anyLong())
    }

    @Test
    fun `reportSourceFailed returns source after reporting`() {
        val src = Source(id = 1L, failCount = 0)
        `when`(sourceDao.getById(1L)).thenReturn(src)
        val result = repo.reportSourceFailed(1L)
        assert(result == src)
    }

    @Test
    fun `reportSourceFailed returns null when source not found`() {
        `when`(sourceDao.getById(1L)).thenReturn(null)
        val result = repo.reportSourceFailed(1L)
        assert(result == null)
    }

    @Test
    fun `reportSourceSuccess resets failCount and status`() {
        repo.reportSourceSuccess(1L, 1000)
        verify(sourceDao).markAsSuccess(eq(1L), eq(1000), anyLong())
    }

    @Test
    fun `reportSourceSuccess applies priority bonus for fast source under 500ms`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, priority = 100))
        repo.reportSourceSuccess(1L, 300)
        verify(sourceDao).updatePriority(eq(1L), eq(85), anyLong())
    }

    @Test
    fun `reportSourceSuccess applies priority penalty for slow source over 3000ms`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, priority = 100))
        repo.reportSourceSuccess(1L, 5000)
        verify(sourceDao).updatePriority(eq(1L), eq(110), anyLong())
    }

    @Test
    fun `reportSourceSuccess does not change priority for normal speed`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, priority = 100))
        repo.reportSourceSuccess(1L, 1500)
        verify(sourceDao, never()).updatePriority(anyLong(), anyInt(), anyLong())
    }

    @Test
    fun `reportSourceSuccess clamps priority to minimum of 1`() {
        `when`(sourceDao.getById(1L)).thenReturn(Source(id = 1L, priority = 10))
        repo.reportSourceSuccess(1L, 200)
        verify(sourceDao).updatePriority(eq(1L), eq(1), anyLong())
    }

    @Test
    fun `getNextAvailableSource delegates to dao`() {
        repo.getNextAvailableSource(1L, 2L)
        verify(sourceDao).getNextAvailableSource(1L, 2L)
    }

    @Test
    fun `getBestSource delegates to dao`() {
        repo.getBestSource(1L)
        verify(sourceDao).getBestSource(1L)
    }

    @Test
    fun `hasAvailableSource returns true when count above zero`() {
        `when`(sourceDao.countAvailableByChannelId(1L)).thenReturn(2)
        assert(repo.hasAvailableSource(1L))
    }

    @Test
    fun `hasAvailableSource returns false when count is zero`() {
        `when`(sourceDao.countAvailableByChannelId(1L)).thenReturn(0)
        assert(!repo.hasAvailableSource(1L))
    }
}
