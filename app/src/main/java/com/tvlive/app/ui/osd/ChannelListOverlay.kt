package com.tvlive.app.ui.osd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tvlive.app.R
import com.tvlive.app.data.db.entity.Channel
import com.tvlive.app.ui.presenter.ChannelListPresenter

class ChannelListOverlay(
    private val container: ViewGroup,
    private val presenter: ChannelListPresenter,
    private val osdManager: OsdManager,
    private val onChannelSelected: (Channel) -> Unit,
    private val getCurrentChannelId: () -> Long
) {

    private var rootView: View? = null
    private lateinit var recycler: RecyclerView
    private lateinit var categoryTabs: LinearLayout
    private lateinit var adapter: ChannelListAdapter
    private var isShowing = false

    private val autoHideRunnable = Runnable { close() }

    fun show() {
        if (isShowing) return
        if (rootView == null) {
            rootView = LayoutInflater.from(container.context)
                .inflate(R.layout.channel_list_overlay_content, container, true)
            recycler = rootView!!.findViewById(R.id.channel_recycler)
            categoryTabs = rootView!!.findViewById(R.id.category_tabs)
            setupRecycler()
            setupCategoryTabs()
        }
        refreshData()
        container.visibility = View.VISIBLE
        isShowing = true
        osdManager.show(OsdManager.OsdState.CHANNEL_LIST)
        scheduleAutoHide()
    }

    fun close() {
        container.visibility = View.GONE
        isShowing = false
        osdManager.hide()
        cancelAutoHide()
    }

    fun refreshData() {
        presenter.init()
        adapter.updateData(
            presenter.getCurrentChannels(),
            getCurrentChannelId(),
            presenter.getCurrentChannels().map { it.id }
                .filter { presenter.isFavorite(it) }.toSet()
        )
        updateCategoryTabHighlight()
    }

    fun isVisible(): Boolean = isShowing

    private fun setupRecycler() {
        adapter = ChannelListAdapter(
            channels = presenter.getCurrentChannels(),
            currentChannelId = getCurrentChannelId(),
            favoriteIds = emptySet(),
            onChannelClick = { channel ->
                onChannelSelected(channel)
                close()
            }
        )
        recycler.layoutManager = LinearLayoutManager(container.context)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)
        recycler.setItemViewCacheSize(5)
    }

    private fun setupCategoryTabs() {
        categoryTabs.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        for (cat in presenter.getCategories()) {
            val tab = inflater.inflate(R.layout.channel_list_item, categoryTabs, false) as TextView
            tab.text = cat
            tab.setTextColor(0xFFFFFFFF.toInt())
            tab.textSize = 16f
            tab.setPadding(24, 12, 24, 12)
            tab.isFocusable = true
            tab.setOnClickListener { selectCategory(cat) }
            tab.setBackgroundColor(0x00000000.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            categoryTabs.addView(tab, lp)
        }
        updateCategoryTabHighlight()
    }

    private fun selectCategory(category: String) {
        presenter.selectCategory(category)
        adapter.updateData(
            presenter.getCurrentChannels(),
            getCurrentChannelId(),
            presenter.getCurrentChannels().map { it.id }
                .filter { presenter.isFavorite(it) }.toSet()
        )
        updateCategoryTabHighlight()
        recycler.scrollToPosition(0)
        scheduleAutoHide()
    }

    private fun updateCategoryTabHighlight() {
        val current = presenter.getCurrentCategory()
        for (i in 0 until categoryTabs.childCount) {
            val tab = categoryTabs.getChildAt(i) as? TextView ?: continue
            val isSelected = tab.text.toString() == current
            tab.setBackgroundColor(
                if (isSelected) 0x66FFFFFF.toInt() else 0x00000000.toInt()
            )
        }
    }

    private fun scheduleAutoHide() {
        cancelAutoHide()
        container.postDelayed(autoHideRunnable, 5000)
    }

    private fun cancelAutoHide() {
        container.removeCallbacks(autoHideRunnable)
    }

    fun onKeyBack(): Boolean {
        if (!isShowing) return false
        close()
        return true
    }
}
