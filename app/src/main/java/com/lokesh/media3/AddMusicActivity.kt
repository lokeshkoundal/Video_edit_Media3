package com.lokesh.media3

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.lokesh.media3.databinding.ActivityAddMusicBinding
import java.io.File

@UnstableApi
class AddMusicActivity : AppCompatActivity(),Transformer.Listener {
    private lateinit var binding : ActivityAddMusicBinding
    private var selectedAudioUri: String = ""
    private var videoUrl: String = ""

    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null

    private var fileName : String? = null
    private var filePath : File? = null
    private var transformer : Transformer? = null
    private var playbackPosition =  0L
    private var playWhenReady = true

    private var editedMediaItemList = mutableListOf<EditedMediaItem>()
    private var audioEditedMediaItemList = mutableListOf<EditedMediaItem>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.addMusicBtn.setOnClickListener {
            pickAudioFromStorage()

        }

        binding.addVideoBtn.setOnClickListener {
            launchNewVideoPicker()

        }

        binding.mergeBtn.setOnClickListener {
            setUpTransformer()
        }

    }

    private fun setUpTransformer(){
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        binding.outputPlayerView.player = null

        kotlin.run {
            mergeAudioVideo()
        }
    }

    private fun mergeAudioVideo() {
        binding.progressBar.visibility = View.VISIBLE

        transformer = Transformer.Builder(this)
            .addListener(this@AddMusicActivity)
            .build()

        audioEditedMediaItemList.add(EditedMediaItem.Builder(
            MediaItem.fromUri(selectedAudioUri))
            .build())

        editedMediaItemList.add(EditedMediaItem.Builder(
            MediaItem.fromUri(videoUrl))
            .setRemoveAudio(binding.checkbox.isChecked)
            .build())

        val mediaItemSequence = EditedMediaItemSequence.Builder(editedMediaItemList).build()

        val backgroundAudioSequence =  EditedMediaItemSequence.Builder(audioEditedMediaItemList)
            .setIsLooping(true)
            .build()

        val composition = Composition.Builder(mediaItemSequence,backgroundAudioSequence).build()

        filePath = createExternalFile()
        transformer!!.start(composition,filePath!!.absolutePath)

    }

    private fun createExternalFile(): File? {
        return try{
            fileName = "Media3_" + System.currentTimeMillis().toString()
            val file = File(cacheDir,"$fileName")
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

        val mediaItem = videoUrl.let { MediaItem.fromUri(it) }

        inputPlayer?.setMediaItem(mediaItem)
        inputPlayer?.seekTo(playbackPosition)
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayer?.prepare()

    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri.toString()
            binding.musicSelected.visibility =View.VISIBLE
            Toast.makeText(this, "Audio Selected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No Audio Selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Call this function when the user clicks a button
    private fun pickAudioFromStorage() {
        pickAudioLauncher.launch("audio/*") // Show only audio files
    }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)

        binding.progressBar.visibility = View.GONE
        binding.outputPlayerView.visibility = View.VISIBLE

        editedMediaItemList.clear()
        audioEditedMediaItemList.clear()
        videoUrl=""
        selectedAudioUri = ""

        initOutputPlayer()

    }

    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        binding.outputPlayerView.player = outputPlayer

        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }

    override fun onError(
        composition: Composition,
        exportResult: ExportResult,
        exportException: ExportException
    ) {
        super.onError(composition, exportResult, exportException)
        binding.outputPlayerView.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()

    }

    override fun onStop() {
        super.onStop()
        filePath?.let {
            if (it.exists()) {
                it.delete()
                Log.d("MergeVidsActivity", "Temporary file deleted: ${it.absolutePath}")
            }
        }
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
}