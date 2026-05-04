package com.tvlive.app.ui.osd

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.*

class OsdManagerTest {

    private lateinit var handler: android.os.Handler
    private lateinit var osdManager: OsdManager

    @Before
    fun setUp() {
        handler = mock(android.os.Handler::class.java)
        osdManager = OsdManager(handler)
    }

    @Test
    fun `initial state is idle`() {
        assertEquals(OsdManager.OsdState.IDLE, osdManager.state)
    }

    @Test
    fun `show sets state to channel info`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO)
        assertEquals(OsdManager.OsdState.CHANNEL_INFO, osdManager.state)
    }

    @Test
    fun `show sets state to channel list`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_LIST)
        assertEquals(OsdManager.OsdState.CHANNEL_LIST, osdManager.state)
    }

    @Test
    fun `show transitions from channel info to settings`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO)
        osdManager.show(OsdManager.OsdState.SETTINGS)
        assertEquals(OsdManager.OsdState.SETTINGS, osdManager.state)
    }

    @Test
    fun `hide returns to idle`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO)
        osdManager.hide()
        assertEquals(OsdManager.OsdState.IDLE, osdManager.state)
    }

    @Test
    fun `show with autoHideMs posts delayed runnable`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO, 3000L)
        verify(handler).postDelayed(any(Runnable::class.java), eq(3000L))
    }

    @Test
    fun `show without autoHideMs does not post delayed`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO)
        verify(handler, never()).postDelayed(any(Runnable::class.java), anyLong())
    }

    @Test
    fun `show twice cancels previous autoHide`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO, 3000L)
        osdManager.show(OsdManager.OsdState.CHANNEL_LIST, 5000L)
        // 第一次的 postDelayed 被移除
        // 第二次的 postDelayed 被添加
        verify(handler).removeCallbacks(any(Runnable::class.java))
        verify(handler, times(2)).postDelayed(any(Runnable::class.java), anyLong())
    }

    @Test
    fun `hide removes pending autoHide`() {
        osdManager.show(OsdManager.OsdState.CHANNEL_INFO, 3000L)
        osdManager.hide()
        verify(handler).removeCallbacks(any(Runnable::class.java))
    }

    @Test
    fun `idle transitions skip autoHide cleanup`() {
        osdManager.show(OsdManager.OsdState.IDLE)
        assertEquals(OsdManager.OsdState.IDLE, osdManager.state)
    }
}
