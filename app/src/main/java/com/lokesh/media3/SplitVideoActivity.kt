package com.lokesh.media3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.lokesh.media3.databinding.ActivitySplitVideoBinding
import java.io.File

@UnstableApi
class SplitVideoActivity : AppCompatActivity(),Transformer.Listener {

    lateinit var binding : ActivitySplitVideoBinding

    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer1 : ExoPlayer? = null
    private var outputPlayer2 : ExoPlayer? = null

    private var playbackPosition = 0L
    private var playWhenReady = true

    private var fileName1 : String? = null
    private var fileName2 : String? = null
    private var filePath1 : File? = null
    private var filePath2 : File? = null

    private var transformer1 : Transformer? = null
    private var transformer2 : Transformer? = null
    private var videoUrl : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplitVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.addVideoBtn.setOnClickListener {
            launchNewVideoPicker()
        }

        binding.splitVideoBtn.setOnClickListener {

            if(binding.slider.value>2&&binding.slider.valueTo - binding.slider.value>2){
                setUpTransformer()
            }else{
                Toast.makeText(this,"Invalid values",Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setUpTransformer() {
        outputPlayer1?.stop()
        outputPlayer2?.stop()

        outputPlayer1?.release()
        outputPlayer2?.release()

        outputPlayer1 = null
        outputPlayer2 = null

        binding.outputPlayerView1.player = null
        binding.outputPlayerView2.player = null

        binding.progressBar1.visibility = View.VISIBLE
        binding.progressBar2.visibility = View.VISIBLE

        splitVideo()

    }

    private fun splitVideo() {

        transformer1 = Transformer
            .Builder(this)
            .addListener(this)
            .build()

        transformer2 = Transformer
            .Builder(this)
            .addListener(this)
            .build()

        val inputMediaItem1 = MediaItem
            .Builder()
            .setUri(videoUrl)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs((binding.slider.value * 1000).toLong())
                    .build()
            )
            .build()


        val inputMediaItem2 = MediaItem
            .Builder()
            .setUri(videoUrl)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs((binding.slider.value*1000).toLong())
                    .setEndPositionMs((binding.slider.valueTo*1000).toLong())
                    .build()
            )
            .build()

        val editedMediaItem1 = EditedMediaItem.Builder(inputMediaItem1).build()
        val editedMediaItem2 = EditedMediaItem.Builder(inputMediaItem2).build()

        filePath1 = createExternalFile(1)
        filePath2 = createExternalFile(2)

        transformer1!!.start(editedMediaItem1,filePath1!!.absolutePath)
        transformer2!!.start(editedMediaItem2,filePath2!!.absolutePath)
    }

    private fun launchNewVideoPicker() {
        newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUrl = uri.toString()
            if (Util.SDK_INT >= 24) {
                initInputPlayer()
                binding.inputPlayerView.onResume()

            }
        }
    }



    private fun createExternalFile(fileno: Int): File? {
         try{
            if(fileno==1) {
                fileName1 = "Media3_" + System.currentTimeMillis().toString()
                val file = File(cacheDir, "$fileName1")

                check(!(file.exists() && !file.delete())) {
                    "could not delete the previous transformer output file"
                }
                check(file.createNewFile()) { "could not create the transformer output file" }
                return file
            }else{
                fileName2 = "Media3_" + System.currentTimeMillis().toString()
                val file = File(cacheDir, "$fileName2")

                check(!(file.exists() && !file.delete())) {
                    "could not delete the previous transformer output file"
                }
                check(file.createNewFile()) { "could not create the transformer output file" }
                return file
            }

        }catch (e:Exception){
            Toast.makeText(this,
                "could not create the transformer output file ${e.message}",
                Toast.LENGTH_SHORT).show()
                return null
        }
     }

    private fun initInputPlayer() {
        inputPlayer = ExoPlayer.Builder(this).build()
        inputPlayer?.playWhenReady = true
        binding.inputPlayerView.player = inputPlayer

        val mediaItem = videoUrl?.let { MediaItem.fromUri(it) }

        if(mediaItem!=null){
            inputPlayer?.setMediaItem(mediaItem)
        }
        inputPlayer?.seekTo(playbackPosition)
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayer?.prepare()

        inputPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val duration = inputPlayer?.duration?.div(1000f)
                    if (duration != null) {
                        if (duration > 0) {
                            binding.slider.valueTo = duration
                        }
                    }
                }
            }
        })

    }

    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
        binding.outputPlayerView1.visibility = View.GONE
        binding.outputPlayerView2.visibility = View.GONE

        binding.progressBar1.visibility = View.GONE
        binding.progressBar2.visibility = View.GONE

        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
    }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)

        initOutputPlayers()

//        saveBtn.setOnClickListener {
//            if(fileName!=null &&filePath!=null){
//                lifecycleScope.launch {
//                    progressBar.visibility = View.VISIBLE
//
//                    addVideoToGalleryAPI29(filePath!!,fileName!!)
//                    Toast.makeText(this@MainActivity,"Video Saved",Toast.LENGTH_SHORT).show()
//                    progressBar.visibility = View.GONE
//                }
//            }
//        }

    }

    private fun initOutputPlayers() {

        outputPlayer1 = ExoPlayer.Builder(this).build()
        outputPlayer1?.playWhenReady = true
        binding.outputPlayerView1.player = outputPlayer1

        val mediaItem1 = MediaItem.fromUri("file://$filePath1")
        outputPlayer1!!.setMediaItem(mediaItem1)
        outputPlayer1!!.prepare()


        outputPlayer2 = ExoPlayer.Builder(this).build()
        outputPlayer2?.playWhenReady = true
        binding.outputPlayerView2.player = outputPlayer2

        val mediaItem2 = MediaItem.fromUri("file://$filePath2")
        outputPlayer2?.setMediaItem(mediaItem2)
        outputPlayer2?.prepare()

        outputPlayer1!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    binding.progressBar1.visibility = View.GONE
                    binding.outputPlayerView1.visibility = View.VISIBLE
                }
            }
        })

        outputPlayer2!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    binding.progressBar2.visibility = View.GONE
                    binding.outputPlayerView2.visibility = View.VISIBLE
                }
            }
        })


    }

    override fun onStop() {
        super.onStop()
        filePath1?.let {
            if (it.exists()) {
                it.delete()
                Log.d("MergeVidsActivity", "Temporary file deleted: ${it.absolutePath}")
            }
        }

        filePath2?.let {
            if (it.exists()) {
                it.delete()
                Log.d("MergeVidsActivity", "Temporary file deleted: ${it.absolutePath}")
            }
        }
    }

}