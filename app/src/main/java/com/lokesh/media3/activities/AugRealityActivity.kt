package com.lokesh.media3.activities

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
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
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

class AugRealityActivity : AppCompatActivity() {
    private lateinit var sceneView: ARSceneView
//    private lateinit var videoNode: VideoNode
    private lateinit var mediaPlayer: MediaPlayer
    val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
    
    
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
        sceneView =findViewById<ARSceneView>(R.id.sceneView).apply {
            configureSession { session, config ->
                config.addAugmentedImage(
                    session, "luffy",
                    assets.open("luffy2.jpg")
                        .use(BitmapFactory::decodeStream)
                )
                config.addAugmentedImage(
                    session, "copy",
                    assets.open("copy.png")
                        .use(BitmapFactory::decodeStream)
                )
            }
            onSessionUpdated = { session, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                            when (augmentedImage.name) {
                                "luffy" -> addChildNode(
                                    ModelNode(
                                        modelInstance = modelLoader.createModelInstance(
                                            assetFileLocation = "models/sofa.glb"
                                        ),
                                        scaleToUnits = 0.1f,
                                        centerOrigin = Position(0.0f)
                                    )
                                )

                                "copy" -> {
                                 addChildNode(
                                        ExoPlayerNode(
                                            engine = engine,
                                            materialLoader = materialLoader,
//                                            size = Size(x = augmentedImage.extentX, y = augmentedImage.extentZ), // When the width of the image is set
                                            exoPlayer = ExoPlayer.Builder(this@AugRealityActivity).build()
                                                .apply {
                                                    setMediaItem(
                                                        MediaItem.fromUri(
                                                        Uri.parse("android.resource://com.lokesh.media3/${R.raw.ad}")
                                                    ))
                                                    prepare()
                                                    playWhenReady = true
                                                    repeatMode = Player.REPEAT_MODE_ALL
                                                },
//                                            chromaKeyColor = if (chromaKey) 0x2fff19 else null, // 0x2fff19 is colorOf(0.1843f, 1.0f, 0.098f)
                                        )
                                    )}

//                                ).also { qrCodeNode ->
//                                    onTrackingStateChanged = { trackingState ->
//                                        when (trackingState) {
//                                            TrackingState.TRACKING -> {
//                                                if (!qrCodeNode.player.isPlaying) {
//                                                    qrCodeNode.player.start()
//                                                }
//                                            }
//
//                                            else -> {
//                                                if (qrCodeNode.player.isPlaying) {
//                                                    qrCodeNode.player.pause()
//                                                }
//                                            }
//                                        }
//                                    }
//                                })
                            }
                        }
                        addChildNode(augmentedImageNode)
                        augmentedImageNodes += augmentedImageNode
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}