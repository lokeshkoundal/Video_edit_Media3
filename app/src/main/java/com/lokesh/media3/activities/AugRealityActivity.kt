package com.lokesh.media3.activities

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lokesh.media3.R
import com.lokesh.media3.adapters.ExoPlayerNode
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Size

class AugRealityActivity : AppCompatActivity() {
    private lateinit var sceneView: ARSceneView
    private val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
    private lateinit var exoPlayer: ExoPlayer  // Store reference to ExoPlayer
    
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aug_reality)
        
        findViewById<ImageButton>(R.id.videoRestartBtn).setOnClickListener {
            if (::exoPlayer.isInitialized) {
                exoPlayer.seekTo(0)  // Seek to the beginning
                exoPlayer.playWhenReady = true  // Ensure playback starts
            }
        }
        
        
        sceneView = findViewById<ARSceneView>(R.id.sceneView).apply {
            planeRenderer.isVisible = false
            
            exoPlayer = ExoPlayer.Builder(this@AugRealityActivity).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://$packageName/raw/copy_vid")))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
            
            configureSession { session, config ->
                
                config.addAugmentedImage(
                    session, "copy",
                    assets.open("copy.jpg")
                        .use(BitmapFactory::decodeStream),
                    widthInMeters = 0.2f
                )
            }
            
            onSessionFailed = {e->
                Toast.makeText(this@AugRealityActivity,e.toString(),Toast.LENGTH_SHORT).show()
            }
            
            onSessionUpdated = { _, frame ->
                val augmentedImage = frame.getUpdatedAugmentedImages().lastOrNull { it.name == "copy" }
                if (augmentedImage != null && augmentedImageNodes.none { it.imageName == "copy" }) {
                    var width = augmentedImage.extentX // Prevents zero width
                    var height = augmentedImage.extentZ // Prevents zero height
                    Log.d("AR_DEBUG", "Detected Image - Size: width=$width, height=$height")
                    
                    if (height > width) {
                        val temp = width
                        width = height
                        height = temp
                    }
                    
                    val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                        
                        val videoNode = ExoPlayerNode(
                                engine = engine,
                                materialLoader = materialLoader,
                                size = Size(x = width, y = 0.0f, z = height), // Setting the width of the image
                                exoPlayer = exoPlayer
                            )
                        
                        //rotated to match portrait copy
                        videoNode.rotation = videoNode.rotation.copy(y = -90.0f)
                        addChildNode(videoNode)
                    }
                    
                    addChildNode(augmentedImageNode)
                    augmentedImageNodes += augmentedImageNode
                }
            }
            
        }
        
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.stop()
        exoPlayer.release()
    }
    
    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
        exoPlayer.release()
    }
}