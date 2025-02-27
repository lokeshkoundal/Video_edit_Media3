package com.lokesh.media3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.effect.GaussianBlur
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.lokesh.media3.databinding.ActivityEffectsBinding
import java.io.File

@UnstableApi
class EffectsActivity : AppCompatActivity(),Transformer.Listener {
    private lateinit var binding: ActivityEffectsBinding
    
    private var inputPlayer: ExoPlayer? = null
    private var outputPlayer: ExoPlayer? = null
    
    
    private var fileName: String? = null
    
    private var transformer: Transformer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var filePath: File? = null
    
    
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
            
            binding.outputPlayerView.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            
            addEffect()
        }
    }
    
    private fun addEffect() {
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
            
            binding.sepiaRadio.id -> {
                
                val sepiaMatrix = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f,  // Red channel
                    0.349f, 0.686f, 0.168f, 0f,  // Green channel
                    0.272f, 0.534f, 0.131f, 0f,  // Blue channel
                    0f,      0f,      0f,    1f   // Alpha channel
                )
                
                val videoEffect = RgbMatrix{ _, _ ->  sepiaMatrix }
                effects.add(videoEffect)
            }
            
            binding.hslAdjustRadio.id -> {
                val videoEffect = HslAdjustment.Builder()
                    .adjustHue(70f)
                    .adjustSaturation(60f)
                    .adjustLightness(50f)
                    .build()
                
                effects.add(videoEffect)
            }
            
            
            binding.blurRadio.id->{
//                val videoEffect = Presentation.createForWidthAndHeight(480,800,
//                    Presentation.LAYOUT_SCALE_TO_FIT
//                )
                val videoEffect = GaussianBlur(20f)
                effects.add(videoEffect)
            }
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
        binding.outputPlayerView.visibility = View.VISIBLE
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
        binding.outputPlayerView.player = outputPlayer
        
        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }
    
    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
        binding.outputPlayerView.visibility = View.GONE
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