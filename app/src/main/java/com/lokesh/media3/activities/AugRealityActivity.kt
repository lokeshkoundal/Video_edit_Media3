package com.lokesh.media3.activities

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
//    private lateinit var videoNode: VideoNode
//    private lateinit var mediaPlayer: MediaPlayer
    private val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aug_reality)
//
//        sceneView = findViewById<ARSceneView>(R.id.sceneView).apply {
////            this.lightEstimationMode = Config.LightEstimationMode.DISABLED
//        }
//
//        // ✅ Load the target image from assets
//        val bitmap = BitmapFactory.decodeStream(assets.open("copy.jpg")) // Replace with your image file
//
//        // ✅ Initialize MediaPlayer for video playback
//        mediaPlayer = MediaPlayer.create(this, R.raw.animation)
//
//        // ✅ Create Augmented Image Node (Detects Target Photo Frame)
//        val imageNode = AugmentedImageNode(
//            engine = sceneView.engine,
//            imageName = "copy.jpg",  // Name should match the Augmented Image Database
//            bitmap = bitmap,             // Image to track
//            widthInMeters = 0.3f         // Adjust based on the real-world size of the frame
//        ).apply {
//            onUpdate = { node, augmentedImage ->
//                if (augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING) {
//                    placeVideo(node)
//                }
//            }
//        }
//
//        // ✅ Add image node to AR Scene
//        sceneView.addChild(imageNode)
        augImage()
    }
    
    // ✅ Places the video on the detected photo frame
//    private fun placeVideo(imageNode: AugmentedImageNode) {
//        if (!::videoNode.isInitialized) {
//            videoNode = VideoNode(
//                engine = sceneView.engine,
//                scaleToUnits = 0.5f, // Adjust size
//                centerOrigin = Position(y = 0f, x = 0f,z = 0f), // Adjust placement
//                glbFileLocation = "models/plane.glb", // Flat surface model for video
//                player = mediaPlayer,
//                onLoaded = { _, _ ->
//                    mediaPlayer.start()
//                }
//            )
//            imageNode.addChild(videoNode) // Attach video to the detected image frame
//        }
//    }
    
    private fun augImage(){
        sceneView = findViewById<ARSceneView>(R.id.sceneView).apply {
            configureSession { session, config ->
                config.addAugmentedImage(
                    session, "luffy",
                    assets.open("luffy2.jpg")
                        .use(BitmapFactory::decodeStream)
                    , widthInMeters = 0.2f
                )
                
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
           
            onSessionUpdated = { session, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                            when (augmentedImage.name) {
//                                "luffy" -> addChildNode(
//                                    ModelNode(
//                                        modelInstance = modelLoader.createModelInstance(
//                                            assetFileLocation = "models/sofa.glb"
//                                        ),
//                                        scaleToUnits = 0.1f,
//                                        centerOrigin = Position(0.0f)
//                                    )
//                                )

                                "copy" -> {
                                    val width = maxOf(augmentedImage.extentX, 0.1f) // Prevents zero width
                                    val height = maxOf(augmentedImage.extentZ, 0.1f) // Prevents zero height
                                    Log.d("AR_DEBUG", "Detected Image - Size: width=$width, height=$height")
                                    
                                    addChildNode(
                                        ExoPlayerNode(
                                            engine = engine,
                                            materialLoader = materialLoader,
                                            size = Size(x = width, y = 0.0f , z =  height), // When the width of the image is set
                                            exoPlayer = ExoPlayer.Builder(this@AugRealityActivity).build()
                                                .apply {
                                                    setMediaItem(
                                                        MediaItem.fromUri(
                                                        Uri.parse("android.resource://$packageName/raw/copy_vid")
                                                    ))
                                                    prepare()
                                                    playWhenReady = true
                                                    repeatMode = Player.REPEAT_MODE_ALL
                                                },
                                        )
                                    )}

                            }
                        }
                        addChildNode(augmentedImageNode)
                        augmentedImageNodes += augmentedImageNode
                    }
                }
            }
        }
    }
    
}