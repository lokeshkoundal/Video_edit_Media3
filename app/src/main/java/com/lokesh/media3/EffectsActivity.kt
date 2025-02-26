package com.lokesh.media3

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.effect.RgbFilter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.google.android.material.slider.RangeSlider
import com.lokesh.media3.databinding.ActivityEffectsBinding
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class EffectsActivity : AppCompatActivity(),Transformer.Listener {
    private lateinit var binding: ActivityEffectsBinding
    
    private var inputPlayer: ExoPlayer? = null
    private var outputPlayer: ExoPlayer? = null
    
    private var inputPlayerView: PlayerView? = null
    private var outputPlayerView: PlayerView? = null
    private var rangeSlider: RangeSlider? = null
    
    
    private var fileName: String? = null
    
    private var transformer: Transformer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var filePath: File? = null
    
    private var startMs: Float = 0f
    private var endMs: Float = 0f
    
    private val REQUIRED_PERMISSIONS = mutableListOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
    ).toTypedArray()
    
    private var videoUrl: String? = null
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEffectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.addVideoBtn.setOnClickListener {
            launchNewVideoPicker()
        }
        
        binding.startEffectTransformationBtn.setOnClickListener {
            outputPlayer?.stop()
            outputPlayer?.release()
            
            outputPlayer = null
            
            binding.outputPlayerView.player = null
            
            binding.progressBar.visibility = View.VISIBLE
            
            addEffect()
        }
        
        
    }
    
    private fun addEffect() {
        binding.progressBar.visibility = View.VISIBLE
        val effects :MutableList<Effect> = mutableListOf()
        
        
        transformer = Transformer
            .Builder(this)
            .addListener(this)
            .build()
        
        val inputMediaItem = MediaItem
            .Builder()
            .setUri(videoUrl)
            .build()
        
        when(binding.radioGroup.checkedRadioButtonId){
            
            binding.grayscaleRadio.id -> {
                val videoEffect = RgbFilter.createGrayscaleFilter()
                effects.add(videoEffect)
            }
            
            binding.invertedRadio.id -> {
                val videoEffect = RgbFilter.createInvertedFilter()
                effects.add(videoEffect)
            }
            
//            binding.invert.id -> {
//                val videoEffect =
//                effects.add(videoEffect)
//            }
//
//            binding.blur.id -> {
//                val videoEffect = RgbFilter.createBlurFilter()
//                effects.add(videoEffect)
//            }
//
//            binding.sharpen.id -> {
//                val videoEffect = RgbFilter.createSharpenFilter()
//                effects.add(videoEffect)
//            }
//
//            binding.vignette.id -> {
//                val videoEffect = RgbFilter.createVignetteFilter()
//                effects.add(videoEffect)
//            }
            
            
        }
        val videoEffect = RgbFilter.createGrayscaleFilter()
        effects.add(videoEffect)
        
        
        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
            setEffects(Effects(mutableListOf(), effects))
        }
        
        
        filePath = createExternalFile()
        transformer!!.start(editedMediaItem.build(), filePath!!.absolutePath)
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
    
    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        binding.progressBar.visibility = View.GONE
        outputPlayerView?.visibility = View.VISIBLE
        initOutputPlayer()
        
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
    
    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        outputPlayerView?.player = outputPlayer
        
        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }
    
    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
        outputPlayerView?.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
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
        
    }
}