package com.tvlive.app.service

import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Source
import org.junit.Assert.*
import org.junit.Test

class SourceHealthCheckerTest {

    private val checker = SourceHealthChecker()
    private val now = System.currentTimeMillis()
    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    @Test
    fun `shouldDelete returns true when last success was long ago and fail count high`() {
        val source = Source(
            lastSuccessTime = now - 10 * 24 * 60 * 60 * 1000L, // 10 days ago
            failCount = 5
        )
        assertTrue(checker.shouldDelete(source, now, sevenDaysMs))
    }

    @Test
    fun `shouldDelete returns false when fail count is low`() {
        val source = Source(
            lastSuccessTime = now - 10 * 24 * 60 * 60 * 1000L,
            failCount = 3 // below threshold
        )
        assertFalse(checker.shouldDelete(source, now, sevenDaysMs))
    }

    @Test
    fun `shouldDelete returns false when last success was recent`() {
        val source = Source(
            lastSuccessTime = now - 2 * 24 * 60 * 60 * 1000L, // 2 days ago
            failCount = 5
        )
        assertFalse(checker.shouldDelete(source, now, sevenDaysMs))
    }

    @Test
    fun `shouldDelete returns false when last success is null`() {
        val source = Source(
            lastSuccessTime = null,
            failCount = 5
        )
        assertFalse(checker.shouldDelete(source, now, sevenDaysMs))
    }

    @Test
    fun `shouldDelete returns false when both conditions not met`() {
        val source = Source(
            lastSuccessTime = now - 3 * 24 * 60 * 60 * 1000L,
            failCount = 2
        )
        assertFalse(checker.shouldDelete(source, now, sevenDaysMs))
    }
}
