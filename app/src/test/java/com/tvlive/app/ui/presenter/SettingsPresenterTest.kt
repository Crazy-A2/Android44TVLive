package com.tvlive.app.ui.presenter

import com.tvlive.app.data.db.dao.SourceConfigDao
import com.tvlive.app.data.db.entity.SourceConfig
import com.tvlive.app.player.DecoderMode
import com.tvlive.app.util.PreferenceHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SettingsPresenterTest {

    private lateinit var sourceConfigDao: SourceConfigDao
    private lateinit var prefs: PreferenceHelper
    private lateinit var presenter: SettingsPresenter

    @Before
    fun setUp() {
        sourceConfigDao = mock(SourceConfigDao::class.java)
        prefs = mock(PreferenceHelper::class.java)
        presenter = SettingsPresenter(sourceConfigDao, prefs)
    }

    @Test
    fun `getDecoderModes returns all modes`() {
        val modes = presenter.getDecoderModes()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(DecoderMode.AUTO))
        assertTrue(modes.contains(DecoderMode.SOFTWARE))
        assertTrue(modes.contains(DecoderMode.HARDWARE))
    }

    @Test
    fun `getDecoderMode returns AUTO by default`() {
        `when`(prefs.decoderMode).thenReturn("auto")
        assertEquals(DecoderMode.AUTO, presenter.getDecoderMode())
    }

    @Test
    fun `getDecoderMode returns SOFTWARE when set`() {
        `when`(prefs.decoderMode).thenReturn("software")
        assertEquals(DecoderMode.SOFTWARE, presenter.getDecoderMode())
    }

    @Test
    fun `getDecoderMode returns AUTO on invalid value`() {
        `when`(prefs.decoderMode).thenReturn("invalid")
        assertEquals(DecoderMode.AUTO, presenter.getDecoderMode())
    }

    @Test
    fun `setDecoderMode stores the value`() {
        presenter.setDecoderMode(DecoderMode.HARDWARE)
        verify(prefs).decoderMode = "hardware"
    }

    @Test
    fun `getSourceConfigs returns list from dao`() {
        val configs = listOf(
            SourceConfig(id = 1, name = "A", url = "http://a"),
            SourceConfig(id = 2, name = "B", url = "http://b")
        )
        `when`(sourceConfigDao.getAll()).thenReturn(configs)
        assertEquals(2, presenter.getSourceConfigs().size)
    }

    @Test
    fun `addSourceConfig inserts new config`() {
        presenter.addSourceConfig("测试源", "http://test", "m3u")
        verify(sourceConfigDao).insert(any(SourceConfig::class.java))
    }

    @Test
    fun `deleteSourceConfig deletes by id`() {
        `when`(sourceConfigDao.getById(1L)).thenReturn(SourceConfig(id = 1, name = "A", url = "http://a"))
        presenter.deleteSourceConfig(1L)
        verify(sourceConfigDao).delete(any(SourceConfig::class.java))
    }

    @Test
    fun `deleteSourceConfig does nothing if not found`() {
        `when`(sourceConfigDao.getById(1L)).thenReturn(null)
        presenter.deleteSourceConfig(1L)
        verify(sourceConfigDao, never()).delete(any(SourceConfig::class.java))
    }

    @Test
    fun `toggleSourceConfig flips enabled state`() {
        val config = SourceConfig(id = 1, name = "A", url = "http://a", isEnabled = true)
        `when`(sourceConfigDao.getById(1L)).thenReturn(config)
        presenter.toggleSourceConfig(1L)
        assertFalse(config.isEnabled)
        verify(sourceConfigDao).update(config)
    }

    @Test
    fun `getAppVersion returns version string`() {
        assertEquals("1.0.0", presenter.getAppVersion())
    }
}
