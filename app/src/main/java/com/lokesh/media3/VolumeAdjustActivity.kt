package com.lokesh.media3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.lokesh.media3.databinding.ActivityVolumeAdjustBinding
import java.io.File

@UnstableApi
class VolumeAdjustActivity : AppCompatActivity(),Transformer.Listener {


    private lateinit var binding : ActivityVolumeAdjustBinding

    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null

    private var playbackPosition = 0L
    private var playWhenReady = true

    private var fileName : String? = null
    private var filePath : File? = null

    private var transformer : Transformer? = null
    private var videoUrl : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolumeAdjustBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.reduceVolume.setOnClickListener {
            setUpTransformer()

        }

        binding.button.setOnClickListener {
            launchNewVideoPicker()
        }
    }

    private fun launchNewVideoPicker(){
        newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()){ uri->
        videoUrl = uri.toString()
        if(Util.SDK_INT >= 24){
            initInputPlayer()
            binding.inputPlayerView.onResume()
        }
    }

    private fun initInputPlayer() {
        inputPlayer = ExoPlayer.Builder(this).build()
        inputPlayer?.playWhenReady = true
        binding.inputPlayerView.player = inputPlayer
        binding.seekBar.visibility = View.VISIBLE


        val mediaItem = videoUrl?.let { MediaItem.fromUri(it) }

        if(mediaItem!=null){
            inputPlayer?.setMediaItem(mediaItem)
        }
        inputPlayer?.seekTo(playbackPosition)
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayer?.prepare()

    }

    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        binding.outputPlayerView.player = outputPlayer

        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }

    private fun releasePlayer(){
        inputPlayer?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            inputPlayer = null
        }

        outputPlayer?.release()
        outputPlayer = null
    }

    override fun onStop() {
        super.onStop()
        if(Util.SDK_INT>=24){
            releasePlayer()
        }
        filePath?.let {
            if (it.exists()) {
                it.delete()
                Log.d("MergeVidsActivity", "Temporary file deleted: ${it.absolutePath}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.inputPlayerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()

    }

    private fun setUpTransformer(){
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        binding.outputPlayerView.player = null

        adjustVolume()

    }

    /**
     * Adjusts the volume of a video file based on the value of a SeekBar.
     * If the volume is set to 0, it removes the audio from the video.
     * Otherwise, it scales the audio volume by the specified factor.
     */
    private fun adjustVolume() {
        binding.progressBar.visibility = View.VISIBLE
        val volume = binding.seekBar.value/100  // Get volume level from SeekBar (0.0 to 1.0)

        // Create Audio Processor for Volume Adjustment

        if(volume==0.0f){
            transformer = Transformer
                .Builder(this)
                .addListener(this)
                .build()

            val inputMediaItem = MediaItem.Builder()
                .setUri(videoUrl)
                .build()

            val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
                setRemoveAudio(true)  //remove audio
            }

            filePath = createExternalFile()
            transformer!!.start(editedMediaItem.build(), filePath!!.absolutePath)
        }
        else{
            val processors = ImmutableList.Builder<AudioProcessor>()
            val mixingAudioProcessor = ChannelMixingAudioProcessor()

            var inputChannelCount = 1
            while (inputChannelCount <= 6){
                val matrix = ChannelMixingMatrix.create(inputChannelCount,inputChannelCount)
                mixingAudioProcessor.putChannelMixingMatrix(matrix.scaleBy(volume))
                inputChannelCount++
            }

            // Apply the volume adjustment.
            val audioProcessor :MutableList<AudioProcessor> =   processors.add(mixingAudioProcessor).build()

            // Create an Effects object with the volume processor
            val effects = Effects(audioProcessor, listOf())

            transformer = Transformer
                .Builder(this)
                .addListener(this@VolumeAdjustActivity)
                .build()

            val inputMediaItem = MediaItem.Builder()
                .setUri(videoUrl)
                .build()

            val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
                setRemoveAudio(false)  // Don't remove audio
                setEffects(effects)    // Apply volume effects
            }

            filePath = createExternalFile()
            transformer!!.start(editedMediaItem.build(), filePath!!.absolutePath)
        }

    }

    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
        binding.outputPlayerView.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message, Toast.LENGTH_SHORT).show()
    }

    private fun createExternalFile(): File? {
        return try{
            fileName = "Media3_" + System.currentTimeMillis().toString()
            val file = File(externalCacheDir,"$fileName")
            check(!(file.exists() && !file.delete())){
                "could not delete the previous transformer output file"
            }
            check(file.createNewFile()){"could not create the transformer output file"}
            file
        }catch (e:Exception){
            Toast.makeText(this,
                "could not create the transformer output file ${e.message}",
                Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        binding.progressBar.visibility = View.GONE
        binding.outputPlayerView.visibility = View.VISIBLE
        initOutputPlayer()


    }


}