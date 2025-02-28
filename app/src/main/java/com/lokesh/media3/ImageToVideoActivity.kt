package com.lokesh.media3

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.lokesh.media3.databinding.ActivityImageToVideoBinding
import java.io.File

@UnstableApi
class ImageToVideoActivity : AppCompatActivity(),Transformer.Listener {
    private lateinit var binding : ActivityImageToVideoBinding
    private var imageUri : String = ""
    
    
    private var outputPlayer : ExoPlayer? = null
    private var fileName : String? = null
    
    private var transformer : Transformer? = null
    private var filePath : File? = null
    
    private var isRecording = false
    
    
    
    private val newImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//        releasePlayer()
        imageUri = uri.toString()
        binding.imageView.setImageURI(Uri.parse(imageUri))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageToVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.addImageBtn.setOnClickListener {
            newImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        
        binding.createVideoBtn.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            outputPlayer = null
            binding.outputPlayerView.player = null
            binding.outputPlayerView.visibility = View.INVISIBLE
            
            transformImageToVideo()
        }
    }
    
    private fun transformImageToVideo() {
        
        transformer = Transformer.Builder(this)
            .addListener(this)
            .build()
        
        val imageItem  = MediaItem.fromUri(Uri.parse(imageUri))
        
        val editedMediaItem = EditedMediaItem.Builder(imageItem)
            .setDurationUs(binding.secondsEt.text.toString().toLong() * 1000000)
            .setFrameRate(1)
            .build()
        
        filePath = createExternalFile()
        transformer!!.start(editedMediaItem,filePath!!.absolutePath)
        
    }
    
    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        
        binding.progressBar.visibility = View.GONE
        binding.outputPlayerView.visibility = View.VISIBLE
        
        initOutputPlayer()

    }
    
    override fun onError(
        composition: Composition,
        exportResult: ExportResult,
        exportException: ExportException
    ) {
        super.onError(composition, exportResult, exportException)
        
        binding.outputPlayerView.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
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
    
    
    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        binding.outputPlayerView.player = outputPlayer
        
        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }
}