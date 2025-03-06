package com.lokesh.media3.activities

import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.lokesh.media3.R
import com.lokesh.media3.databinding.ActivityVoiceOverBinding
import java.io.File

@UnstableApi
class VoiceOverActivity : AppCompatActivity(),Transformer.Listener {
    private lateinit var binding : ActivityVoiceOverBinding
    
    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null
    private var inputPlayerView : PlayerView? = null
    private var audioFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private var isRecording = false
    
    val silentAudioUri = Uri.parse("android.resource://com.lokesh.media3/raw/silent")
    
    private var transformer : Transformer? = null
    private var filePath : File? = null
    private var videoUrl : String? = null
    private val playWhenReady = true
    private var startTimeInMs= 0L
    private var stopTimeInMs = 0L
    private var fileName : String? = null
    
    private var editedMediaItemList = mutableListOf<EditedMediaItem>()
    private var audioEditedMediaItemList = mutableListOf<EditedMediaItem>()
    
    
    
    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()){uri->
        videoUrl = uri.toString()
        
        if(uri!=null&&Util.SDK_INT >= 24){
            initInputPlayer()
            inputPlayerView?.onResume()
            
        }
       
    }
    
    private fun initInputPlayer() {
        inputPlayer = ExoPlayer.Builder(this).build()
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayerView?.player = inputPlayer
        
        val mediaItem = videoUrl?.let { MediaItem.fromUri(it) }
        
        if(mediaItem!=null){
            inputPlayer?.setMediaItem(mediaItem)
        }
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayer?.prepare()
        
    }
    
    private fun startRecording() {
        isRecording = true
        
        val file = File(cacheDir, "voiceover_${System.currentTimeMillis()}.mp3")
        audioFilePath = file.absolutePath
        
        binding.recordBtn.setImageResource(R.drawable.ic_stop)
        binding.startOrStopRecording.text = "Stop Recording"
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)  // API 31+ (Android 12+)
        } else {
            MediaRecorder()  // API < 31
        }
        
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            prepare()
            start()
        }
        
    }
    
    private fun stopRecording(): String? {
        isRecording = false
        
        binding.recordBtn.setImageResource(R.drawable.ic_mic)
        binding.startOrStopRecording.text = "Start Recording"
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        binding.recordBtn.setImageResource(R.drawable.ic_mic)
        return audioFilePath
    }
    
    
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecording()
            
        } else {
            Toast.makeText(this, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkMicPermissionAndRecord():Boolean {
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED){
            return true
        } else {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceOverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inputPlayerView = binding.inputPlayerView
        
        binding.recordBtn.setOnClickListener {
            
           if(checkMicPermissionAndRecord()){
               if(isRecording){
                   stopRecording()
                   inputPlayer?.pause()
                   stopTimeInMs = inputPlayer?.currentPosition!!
                   Toast.makeText(this,"Recording Stopped at ${stopTimeInMs/1000}",Toast.LENGTH_SHORT).show()
               }
               
               else{
                   if(videoUrl!=null){
                       startRecording()
                       startTimeInMs= inputPlayer?.currentPosition!!
                       Toast.makeText(this,"Recording Started at ${startTimeInMs/1000}",Toast.LENGTH_SHORT).show()
                       
                       inputPlayer?.playWhenReady = true
                       inputPlayer?.prepare()
                       inputPlayer?.play()
                       inputPlayerView?.onResume()
                   }
                  
               }
           }
        }
        
        binding.addVideoBtn.setOnClickListener {
            //error if permission not given
            newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        
        binding.tranformBtn.setOnClickListener {
            if(videoUrl!=null&&audioFilePath!=null){
                transform()
            }else{
                Toast.makeText(this,"No Video or audio",Toast.LENGTH_SHORT).show()
            }
        }
        
    }
    
    private fun transform() {
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        binding.outputPlayerView.player = null
        binding.progressBar.visibility = View.VISIBLE
        
        transformer = Transformer.Builder(this)
            .addListener(this)
            .build()
        
        //for silence
        val silenceAudioItem = MediaItem.Builder()
            .setUri(silentAudioUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)  // Start from beginning
                    .setEndPositionMs(startTimeInMs)  // Set custom duration
                    .build()
            )
            .build()
        
        audioEditedMediaItemList.add(EditedMediaItem.Builder(silenceAudioItem).build())
        
        
        //for recorded audio
        audioEditedMediaItemList.add(
            EditedMediaItem.Builder(
                MediaItem.fromUri(audioFilePath!!))
                .build())
        
        
        editedMediaItemList.add(
            EditedMediaItem.Builder(
            MediaItem.fromUri(videoUrl!!))
            .setRemoveAudio(true)
            .build())
        
        val videoMediaItemSequence = EditedMediaItemSequence.Builder(editedMediaItemList).build()
        
        val backgroundAudioSequence =  EditedMediaItemSequence.Builder(audioEditedMediaItemList)
            .setIsLooping(false)
            .build()
        
        
        val composition = Composition.Builder(videoMediaItemSequence,backgroundAudioSequence).build()
        
        filePath = createExternalFile()
        transformer!!.start(composition,filePath!!.absolutePath)
        
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
        
        editedMediaItemList.clear()
        audioEditedMediaItemList.clear()
        videoUrl=""
        
        initOutputPlayer()
        
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
    
    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        binding.outputPlayerView.player = outputPlayer
        
        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }
    
}