package com.tvlive.app.ui.osd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tvlive.app.R
import com.tvlive.app.data.db.entity.Channel

class ChannelListAdapter(
    private var channels: List<Channel> = emptyList(),
    private var currentChannelId: Long = -1L,
    private var favoriteIds: Set<Long> = emptySet(),
    private val onChannelClick: (Channel) -> Unit = {}
) : RecyclerView.Adapter<ChannelListAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1

    init {
        setHasStableIds(true)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logoImage: ImageView = itemView.findViewById(R.id.item_channel_logo)
        val numberText: TextView = itemView.findViewById(R.id.item_channel_number)
        val nameText: TextView = itemView.findViewById(R.id.item_channel_name)
        val favText: TextView = itemView.findViewById(R.id.item_fav_indicator)
        val root: RelativeLayout = itemView.findViewById(R.id.channel_item_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.channel_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.numberText.text = channel.channelNumber.toString()
        holder.nameText.text = channel.name
        holder.favText.visibility = if (channel.id in favoriteIds) View.VISIBLE else View.GONE

        if (!channel.logoUrl.isNullOrEmpty()) {
            holder.logoImage.visibility = View.VISIBLE
            Glide.with(holder.logoImage.context)
                .load(channel.logoUrl)
                .circleCrop()
                .into(holder.logoImage)
        } else {
            holder.logoImage.visibility = View.GONE
        }

        val isSelected = position == selectedPosition || channel.id == currentChannelId
        holder.root.setBackgroundColor(
            if (isSelected) 0x66FFFFFF.toInt() else 0x00000000.toInt()
        )

        holder.itemView.setOnClickListener {
            selectedPosition = position
            onChannelClick(channel)
        }
    }

    override fun getItemId(position: Int): Long = channels[position].id

    override fun getItemCount(): Int = channels.size

    fun updateData(
        newChannels: List<Channel>,
        newCurrentId: Long,
        newFavoriteIds: Set<Long>
    ) {
        channels = newChannels
        currentChannelId = newCurrentId
        favoriteIds = newFavoriteIds
        selectedPosition = channels.indexOfFirst { it.id == currentChannelId }
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val prev = selectedPosition
        selectedPosition = position
        notifyItemChanged(prev)
        notifyItemChanged(position)
    }
}
