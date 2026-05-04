package com.tvlive.app.data.db

import android.arch.persistence.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.data.db.entity.Favorite
import com.tvlive.app.data.db.entity.Source
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var channelDao: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getTargetContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        channelDao = db
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetById() {
        val channel = Channel(channelNumber = 1, name = "CCTV-1", category = "央视")
        val id = db.channelDao().insert(channel)
        assertTrue(id > 0)
        val loaded = db.channelDao().getById(id)
        assertNotNull(loaded)
        assertEquals("CCTV-1", loaded!!.name)
        assertEquals(1, loaded.channelNumber)
        assertEquals("央视", loaded.category)
    }

    @Test
    fun testGetByNumber() {
        db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        db.channelDao().insert(Channel(channelNumber = 2, name = "CCTV-2"))
        val c1 = db.channelDao().getByNumber(1)
        assertEquals("CCTV-1", c1!!.name)
        val c2 = db.channelDao().getByNumber(2)
        assertEquals("CCTV-2", c2!!.name)
        val missing = db.channelDao().getByNumber(999)
        assertNull(missing)
    }

    @Test
    fun testGetByEpgId() {
        db.channelDao().insert(Channel(epgId = "cctv1", channelNumber = 1, name = "CCTV-1"))
        val loaded = db.channelDao().getByEpgId("cctv1")
        assertNotNull(loaded)
        assertEquals("CCTV-1", loaded!!.name)
    }

    @Test
    fun testGetAllOrdered() {
        db.channelDao().insert(Channel(channelNumber = 3, name = "C", sortOrder = 2))
        db.channelDao().insert(Channel(channelNumber = 1, name = "A", sortOrder = 1))
        db.channelDao().insert(Channel(channelNumber = 2, name = "B", sortOrder = 0))
        val all = db.channelDao().getAllOrdered()
        assertEquals(3, all.size)
        assertEquals("B", all[0].name) // sortOrder 0 first
        assertEquals("A", all[1].name) // sortOrder 1
        assertEquals("C", all[2].name) // sortOrder 2
    }

    @Test
    fun testGetByCategory() {
        db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1", category = "央视"))
        db.channelDao().insert(Channel(channelNumber = 2, name = "CCTV-2", category = "央视"))
        db.channelDao().insert(Channel(channelNumber = 3, name = "湖南卫视", category = "卫视"))
        val yangshi = db.channelDao().getByCategory("央视")
        assertEquals(2, yangshi.size)
        val weishi = db.channelDao().getByCategory("卫视")
        assertEquals(1, weishi.size)
        assertEquals("湖南卫视", weishi[0].name)
    }

    @Test
    fun testCount() {
        assertEquals(0, db.channelDao().count())
        db.channelDao().insert(Channel(channelNumber = 1, name = "A"))
        assertEquals(1, db.channelDao().count())
        db.channelDao().insert(Channel(channelNumber = 2, name = "B"))
        assertEquals(2, db.channelDao().count())
    }

    @Test
    fun testUpdate() {
        val id = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        val channel = db.channelDao().getById(id)!!
        channel.name = "CCTV-1 综合"
        db.channelDao().update(channel)
        val updated = db.channelDao().getById(id)!!
        assertEquals("CCTV-1 综合", updated.name)
    }

    @Test
    fun testDelete() {
        val id = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        db.channelDao().delete(db.channelDao().getById(id)!!)
        assertNull(db.channelDao().getById(id))
    }

    @Test
    fun testFavoriteLifecycle() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        assertFalse(db.favoriteDao().isFavorite(cid))
        db.favoriteDao().insert(Favorite(channelId = cid))
        assertTrue(db.favoriteDao().isFavorite(cid))
        db.favoriteDao().deleteByChannelId(cid)
        assertFalse(db.favoriteDao().isFavorite(cid))
    }

    @Test
    fun testSourceOrderByPriority() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        db.sourceDao().insert(Source(channelId = cid, url = "http://slow", priority = 200))
        db.sourceDao().insert(Source(channelId = cid, url = "http://fast", priority = 100))
        val best = db.sourceDao().getBestSource(cid)!!
        assertEquals("http://fast", best.url)
    }

    @Test
    fun testSourceFailAndSwitch() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        val sid = db.sourceDao().insert(Source(channelId = cid, url = "http://primary", priority = 100))
        db.sourceDao().insert(Source(channelId = cid, url = "http://backup", priority = 200))

        val next = db.sourceDao().getNextAvailableSource(cid, sid)
        assertNotNull(next)
        assertEquals("http://backup", next!!.url)
    }

    @Test
    fun testSourceMarkFailed() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        val sid = db.sourceDao().insert(Source(channelId = cid, url = "http://fail"))

        // 模拟失败 3 次
        for (i in 1..3) {
            db.sourceDao().incrementFailCount(sid)
        }
        db.sourceDao().markAsFailed(sid)

        val best = db.sourceDao().getBestSource(cid)
        assertNull(best) // status=FAILED 被过滤
    }

    @Test
    fun testSourceMarkSuccessAndPriority() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        val sid = db.sourceDao().insert(Source(channelId = cid, url = "http://slow", priority = 100))

        db.sourceDao().markAsSuccess(sid, 200)
        val updated = db.sourceDao().getBestSource(cid)!!
        assertEquals(200, updated.responseTimeMs)
        assertEquals(0, updated.failCount)
    }

    @Test
    fun testHistoryTrim() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        for (i in 1..5) {
            db.historyDao().insert(com.tvlive.app.data.db.entity.History(channelId = cid))
        }
        var all = db.historyDao().getRecent(100)
        assertEquals(5, all.size)
        db.historyDao().trimTo(3)
        all = db.historyDao().getRecent(100)
        assertEquals(3, all.size)
    }

    @Test
    fun testSourceConfigCRUD() {
        val cfg = com.tvlive.app.data.db.entity.SourceConfig(
            name = "测试源", url = "http://example.com/list.m3u", format = "m3u", isBuiltin = false
        )
        val id = db.sourceConfigDao().insert(cfg)
        assertTrue(id > 0)
        val loaded = db.sourceConfigDao().getById(id)
        assertNotNull(loaded)
        assertEquals("测试源", loaded!!.name)
        assertFalse(loaded.isBuiltin)

        loaded.isEnabled = false
        db.sourceConfigDao().update(loaded)
        val afterUpdate = db.sourceConfigDao().getById(id)
        assertFalse(afterUpdate!!.isEnabled)

        db.sourceConfigDao().delete(afterUpdate)
        assertNull(db.sourceConfigDao().getById(id))
    }

    @Test
    fun testFavoriteSequentialCalls() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        db.favoriteDao().insert(Favorite(channelId = cid))
        assertTrue(db.favoriteDao().isFavorite(cid))
        val fav = db.favoriteDao().getByChannelId(cid)
        assertNotNull(fav)
        assertEquals(cid, fav!!.channelId)

        db.favoriteDao().delete(fav)
        assertFalse(db.favoriteDao().isFavorite(cid))
    }

    @Test
    fun testHistoryInsertAndOrder() {
        val cid = db.channelDao().insert(Channel(channelNumber = 1, name = "CCTV-1"))
        val t1 = System.currentTimeMillis()
        Thread.sleep(10)
        db.historyDao().insert(com.tvlive.app.data.db.entity.History(channelId = cid))
        Thread.sleep(10)
        val t2 = System.currentTimeMillis()
        Thread.sleep(10)
        db.historyDao().insert(com.tvlive.app.data.db.entity.History(channelId = cid))

        val recent = db.historyDao().getRecent(2)
        assertEquals(2, recent.size)
        assertTrue(recent[0].watchedAt >= recent[1].watchedAt)
    }
}
