package com.tvlive.app.ui.presenter

import com.tvlive.app.data.db.dao.ChannelDao
import com.tvlive.app.data.db.dao.FavoriteDao
import com.tvlive.app.data.db.dao.SourceDao
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Favorite
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyLong

class ChannelListPresenterTest {

    private lateinit var channelDao: ChannelDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var sourceDao: SourceDao
    private lateinit var presenter: ChannelListPresenter

    @Before
    fun setUp() {
        channelDao = mock(ChannelDao::class.java)
        favoriteDao = mock(FavoriteDao::class.java)
        sourceDao = mock(SourceDao::class.java)
        presenter = ChannelListPresenter(channelDao, favoriteDao, sourceDao)
    }

    @Test
    fun `init loads all channels`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, channelNumber = 1, name = "A"),
            Channel(id = 2, channelNumber = 2, name = "B")
        ))
        presenter.init()
        assertEquals(2, presenter.getAllChannels().size)
        assertEquals(2, presenter.getCurrentChannels().size)
    }

    @Test
    fun `available categories are extracted from channels`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A", category = "央视"),
            Channel(id = 2, name = "B", category = "卫视"),
            Channel(id = 3, name = "C", category = "央视")
        ))
        presenter.init()
        val cats = presenter.getCategories()
        assertTrue(cats.contains("全部"))
        assertTrue(cats.contains("央视"))
        assertTrue(cats.contains("卫视"))
        assertEquals(4, cats.size)
    }

    @Test
    fun `selectCategory filters channels`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A", category = "央视"),
            Channel(id = 2, name = "B", category = "卫视"),
            Channel(id = 3, name = "C", category = "央视")
        ))
        presenter.init()
        presenter.selectCategory("央视")
        val channels = presenter.getCurrentChannels()
        assertEquals(2, channels.size)
        assertTrue(channels.all { it.category == "央视" })
    }

    @Test
    fun `selectCategory all returns all channels`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A", category = "央视"),
            Channel(id = 2, name = "B", category = "卫视")
        ))
        presenter.init()
        presenter.selectCategory("全部")
        assertEquals(2, presenter.getCurrentChannels().size)
    }

    @Test
    fun `favorite category shows only favorited channels`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A"),
            Channel(id = 2, name = "B"),
            Channel(id = 3, name = "C")
        ))
        `when`(favoriteDao.getAll()).thenReturn(listOf(
            Favorite(channelId = 1), Favorite(channelId = 3)
        ))
        presenter.init()
        presenter.selectCategory("收藏")
        assertEquals(2, presenter.getCurrentChannels().size)
    }

    @Test
    fun `selectCategory with no match shows empty list`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A", category = "央视")
        ))
        presenter.init()
        presenter.selectCategory("不存在")
        assertTrue(presenter.getCurrentChannels().isEmpty())
    }

    @Test
    fun `toggleFavorite adds and removes`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A")
        ))
        presenter.init()
        `when`(favoriteDao.isFavorite(1L)).thenReturn(false, true)
        presenter.toggleFavorite(1L)
        verify(favoriteDao).insert((any(Favorite::class.java) as? Favorite) ?: Favorite())
        presenter.toggleFavorite(1L)
        verify(favoriteDao).deleteByChannelId(1L)
    }

    @Test
    fun `getCurrentChannelIndex finds correct position`() {
        `when`(channelDao.getAllIncludingHidden()).thenReturn(listOf(
            Channel(id = 1, name = "A"),
            Channel(id = 2, name = "B"),
            Channel(id = 3, name = "C")
        ))
        presenter.init()
        assertEquals(1, presenter.getCurrentChannelIndex(2L))
        assertEquals(0, presenter.getCurrentChannelIndex(1L))
        assertEquals(-1, presenter.getCurrentChannelIndex(999L))
    }
}
