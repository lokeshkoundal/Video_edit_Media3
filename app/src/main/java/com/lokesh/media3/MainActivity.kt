package com.lokesh.media3

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import androidx.media3.ui.PlayerView
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@UnstableApi
class MainActivity : AppCompatActivity(),Transformer.Listener {

    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null

    private var inputPlayerView : PlayerView? = null
    private var outputPlayerView : PlayerView? = null
    private var rangeSlider : RangeSlider? = null


    private lateinit var progressBar :  ProgressBar
    private lateinit var saveBtn : Button

    private var fileName : String? = null

    private var transformer : Transformer? = null
    private var playbackPosition =  0L
    private var playWhenReady = true
    private var filePath : File? = null

    private var startMs : Float = 0f
    private var endMs : Float = 0f

    private val REQUIRED_PERMISSIONS = mutableListOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
    ).toTypedArray()

    private var videoUrl : String? = null


    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()){uri->
        videoUrl = uri.toString()
        if(Util.SDK_INT >= 24){
            initInputPlayer()
            inputPlayerView?.onResume()
        }
    }

    private fun launchNewVideoPicker(){
        newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    //old
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) { // Check if all permissions are granted
            launchNewVideoPicker()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    //new
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(hasManageExternalStoragePermission()){
                launchNewVideoPicker()
            }else{
                Toast.makeText(this,"Now give Permission",Toast.LENGTH_SHORT).show()
                requestManageExternalStoragePermission()
            }
        }

    private fun requestManageExternalStoragePermission() {
        if (!hasManageExternalStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                resultLauncher.launch(intent)
            } else {
                //BELOW R
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun hasManageExternalStoragePermission() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        }else{
            return false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        inputPlayerView = findViewById(R.id.inputPlayerView1)
        outputPlayerView = findViewById(R.id.inputPlayerView2)
        rangeSlider = findViewById(R.id.rangeSlider)
        saveBtn = findViewById(R.id.saveVideoBtn)

        findViewById<Button>(R.id.removeAudioBtn).setOnClickListener {
            setUpTransformer()
        }


        findViewById<Button>(R.id.pickVideoBtn).setOnClickListener {
           if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
               if(hasManageExternalStoragePermission()){
                   launchNewVideoPicker()
               }else{
                   requestManageExternalStoragePermission()
               }
           }else{
               requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
           }
        }

    }

    private fun initInputPlayer() {
        inputPlayer = ExoPlayer.Builder(this).build()
        inputPlayer?.playWhenReady = true
        inputPlayerView?.player = inputPlayer
        rangeSlider?.visibility = View.VISIBLE


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
                    setupRangeSlider()
                }
            }
        })

    }


    private fun setupRangeSlider() {
        val videoDurationSec = inputPlayer?.duration?.div(1000f)
        rangeSlider?.valueFrom = 0f
        rangeSlider?.valueTo = videoDurationSec!!

        rangeSlider?.values = listOf(0f, videoDurationSec)
        rangeSlider?.addOnChangeListener { slider, _, _ ->
             startMs = slider.values[0]*1000
             endMs = slider.values[1]*1000
            Log.d("Trim", "Trim from $startMs ms to $endMs ms")
        }

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

    private suspend fun addVideoToGalleryAPI29(
        file: File,
        displayName: String): Uri =
            withContext(Dispatchers.IO){
                val savePath = (Environment.DIRECTORY_DCIM + File.separator + file.name)
                val cv = ContentValues()
                cv.put(MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + File.separator + File(savePath).name)

                cv.put(MediaStore.Video.Media.TITLE,"${displayName}.mp4")
                cv.put(MediaStore.Video.Media.DISPLAY_NAME,"${displayName}.mp4")
                cv.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
                cv.put(MediaStore.Video.Media.DATE_ADDED,System.currentTimeMillis()/1000)
                cv.put(MediaStore.Video.Media.DATE_TAKEN,System.currentTimeMillis())
                cv.put(MediaStore.Video.Media.IS_PENDING,1)

                val resolver = contentResolver
                val collection =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uriSavedVideo = resolver.insert(collection,cv)
                val pfd = contentResolver.openFileDescriptor(uriSavedVideo!!,"w")

                if(pfd!=null){
                    try {
                        val out = FileOutputStream(pfd.fileDescriptor)
                        val inputStream = FileInputStream(file)
                        val buf = ByteArray(8192)
                        var len :Int
                        while(inputStream.read(buf).also { len = it } > 0){
                            out.write(buf,0,len)
                        }
                        out.close()
                        inputStream.close()
                        pfd.close()
                        cv.clear()
                        cv.put(MediaStore.Video.Media.IS_PENDING,0)

                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                    contentResolver.update(uriSavedVideo,cv,null,null)

                }
                return@withContext uriSavedVideo
            }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        progressBar.visibility = View.GONE
        outputPlayerView?.visibility = View.VISIBLE
        saveBtn.visibility = View.VISIBLE
        initOutputPlayer()

       saveBtn.setOnClickListener {
           if(fileName!=null &&filePath!=null){
               lifecycleScope.launch {
                   progressBar.visibility = View.VISIBLE

                   addVideoToGalleryAPI29(filePath!!,fileName!!)
                   Toast.makeText(this@MainActivity,"Video Saved",Toast.LENGTH_SHORT).show()
                   progressBar.visibility = View.GONE
               }
           }
       }


    }

    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        outputPlayerView?.player = outputPlayer

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
        inputPlayerView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()

    }

    private fun setUpTransformer(){
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        outputPlayerView?.player= null

        kotlin.run {
            transformVideo()
        }
    }

    private fun transformVideo() {
        progressBar.visibility = View.VISIBLE

        transformer = Transformer
            .Builder(this)
            .addListener(this@MainActivity)
            .build()

        val inputMediaItem = MediaItem
            .Builder()
            .setUri(videoUrl)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.toLong())
                    .setEndPositionMs(endMs.toLong())
                    .build()
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
            setEffects(Effects(mutableListOf(), mutableListOf()))
        }

        filePath = createExternalFile()
        transformer!!.start(editedMediaItem.build(),filePath!!.absolutePath)
    }

    override fun onError(composition: Composition, exportResult: ExportResult,exportException: ExportException) {
        outputPlayerView?.visibility = View.GONE
        progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
    }

}