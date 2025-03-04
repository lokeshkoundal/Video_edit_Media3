package com.lokesh.media3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        if (holder is VideoViewHolder) {
            holder.bind(videoClips[position])
        } else if (holder is AddButtonViewHolder) {
            holder.itemView.setOnClickListener { onAddClick() }
        }
    }

    override fun getItemCount(): Int = videoClips.size

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val thumbnailView: ImageView = view.findViewById(R.id.thumbnailView)
        private val durationView: TextView = view.findViewById(R.id.durationView)

        fun bind(videoClip: VideoClip) {
            durationView.text = "${videoClip.duration / 1000}s"
            // Set first frame as preview
            if (videoClip.thumbnails.isNotEmpty()) {
                thumbnailView.setImageBitmap(videoClip.thumbnails.first())
            }
        }
    }

    class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
