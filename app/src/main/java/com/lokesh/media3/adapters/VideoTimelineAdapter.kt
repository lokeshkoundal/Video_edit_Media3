package com.lokesh.media3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lokesh.media3.R
import com.lokesh.media3.model.OnTrimChangeListener
import com.lokesh.media3.model.VideoClip2
import com.lokesh.media3.view.VideoTimelineView

class VideoTimelineAdapter(private val clips: MutableList<VideoClip2>) :
    RecyclerView.Adapter<VideoTimelineAdapter.TimelineViewHolder>() {
    
    inner class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timelineView: VideoTimelineView = itemView.findViewById(R.id.timeLineView)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_clip, parent, false)
        return TimelineViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val clip = clips[position]
        holder.timelineView.setTrimData(clip, position, object : OnTrimChangeListener {
            override fun onTrimChanged(position: Int, startX: Float, endX: Float) {
                clips[position].startX = startX
                clips[position].endX = endX
                
                // Adjust the next item dynamically
                if (position + 1 < clips.size) {
                    clips[position + 1].startX = endX + 10f  // Add small padding
                }
                
                notifyDataSetChanged() // Refresh RecyclerView
            }
        })
    }
    
    override fun getItemCount(): Int = clips.size
}
