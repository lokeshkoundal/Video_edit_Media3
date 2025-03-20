package com.lokesh.media3.activities

import com.lokesh.media3.adapters.VideoTimelineAdapter
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Transformer
import com.lokesh.media3.R
import com.lokesh.media3.databinding.ActivityVideoEditorBinding
import com.lokesh.media3.model.VideoClip2
import java.io.File

@UnstableApi
class VideoEditorActivity : AppCompatActivity() {
    lateinit var binding : ActivityVideoEditorBinding
    
    private var inputPlayer: ExoPlayer? = null
    private var outputPlayer: ExoPlayer? = null
    
    private var fileName: String? = null
    private var transformer: Transformer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var filePath: File? = null
    
    private val videoClips = mutableListOf<VideoClip2>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.ad}")
        val duration = 10000L // 8 seconds (Replace with actual duration)
        val thumbnails = emptyList<Bitmap>()
        

        repeat(6){
            videoClips.add(VideoClip2(100f,400f))
        }

        val adapter = VideoTimelineAdapter(videoClips)

//        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter
        
        binding.backBtn.setOnClickListener {
            finish()
        }
        
    }
    
    fun extractThumbnails(videoUri: Uri, frameCount: Int = 5): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val interval = durationMs / frameCount
        
        val frames = mutableListOf<Bitmap>()
        for (i in 0 until frameCount) {
            val frame = retriever.getFrameAtTime(i * interval * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
            frame?.let { frames.add(it) }
        }
        retriever.release()
        return frames
    }
}