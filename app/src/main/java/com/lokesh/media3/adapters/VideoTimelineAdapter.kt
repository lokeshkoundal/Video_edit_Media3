package com.lokesh.media3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lokesh.media3.R
import com.lokesh.media3.model.VideoClip

class VideoTimelineAdapter(
    private val videoClips: List<VideoClip>,
    private val onAddClick: () -> Unit // Callback for "+"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_VIDEO = 1
        private const val TYPE_ADD = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (videoClips[position].isAddButton) TYPE_ADD else TYPE_VIDEO
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_VIDEO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_clip, parent, false)
            VideoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_clip, parent, false)
            AddButtonViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener { onAddClick() }
        
    }

    override fun getItemCount(): Int = videoClips.size

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    
    
    }

    class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
