package com.lokesh.media3

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
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
import com.lokesh.media3.databinding.ActivityMergeVidsBinding
import java.io.File

@UnstableApi
class MergeVidsActivity : AppCompatActivity(), Transformer.Listener {

    private var inputPlayer1 : ExoPlayer? = null
    private var inputPlayer2 : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null

    private var inputPlayerView1 : PlayerView? = null
    private var inputPlayerView2 : PlayerView? = null
    private var outputPlayerView : PlayerView? = null

    private lateinit var progressBar : ProgressBar
    private lateinit var addVideoBtn : Button
    private lateinit var mergeVideoBtn : Button

    private var fileName : String? = null

    private var transformer : Transformer? = null
    private var playbackPosition =  0L
    private var playWhenReady = true
    private var filePath : File? = null
    private var videoUrl : MutableList<String> = mutableListOf()
    private lateinit var binding : ActivityMergeVidsBinding

    private val editedMediaItemList = mutableListOf<EditedMediaItem>()

    private val REQUIRED_PERMISSIONS = mutableListOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
    ).toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergeVidsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inputPlayerView1 = binding.inputPlayerView1
        inputPlayerView2 = binding.inputPlayerView2

        outputPlayerView = binding.outputPlayerView
        addVideoBtn = binding.addBtn
        mergeVideoBtn = binding.mergeBtn
        progressBar = binding.progressBar

        binding.addBtn.setOnClickListener {
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

        binding.mergeBtn.setOnClickListener{
            if(videoUrl!!.size<2){
                Toast.makeText(this,"Please add more videos",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setUpTransformer()
        }

    }

    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()){uri->
        if(uri==null){
            return@registerForActivityResult
        }
        videoUrl?.add(uri.toString())
        binding.videosCountTv.text = "Videos Count : " + videoUrl.size.toString()
        if(Util.SDK_INT >= 24){
            if(videoUrl?.size == 1){
                initInputPlayer1()
                inputPlayerView1?.onResume()

            }else if (videoUrl?.size == 2){
                initInputPlayer2()
                inputPlayerView2?.onResume()
            }
        }
    }

    private fun initInputPlayer2() {
        inputPlayer2 = ExoPlayer.Builder(this).build()
        inputPlayer2?.playWhenReady = true
        inputPlayerView2?.player = inputPlayer2


        val mediaItem = videoUrl?.let { MediaItem.fromUri(it[1]) }

        if(mediaItem!=null){
            inputPlayer2?.setMediaItem(mediaItem)
        }
        inputPlayer2?.seekTo(playbackPosition)
        inputPlayer2?.playWhenReady = playWhenReady
        inputPlayer2?.prepare()
    }

    private fun initInputPlayer1() {
        inputPlayer1 = ExoPlayer.Builder(this).build()
        inputPlayer1?.playWhenReady = true
        inputPlayerView1?.player = inputPlayer1


        val mediaItem = videoUrl?.let { MediaItem.fromUri(it[0]) }

        if(mediaItem!=null){
            inputPlayer1?.setMediaItem(mediaItem)
        }
        inputPlayer1?.seekTo(playbackPosition)
        inputPlayer1?.playWhenReady = playWhenReady
        inputPlayer1?.prepare()


    }

    private fun launchNewVideoPicker(){
        newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    //old
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        if (isGranted.containsValue(false)) {
            Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
        }else{
            launchNewVideoPicker()
        }
    }

    //new
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(hasManageExternalStoragePermission()){
                launchNewVideoPicker()
            }else{
                Toast.makeText(this,"Now give Permission", Toast.LENGTH_SHORT).show()
                requestManageExternalStoragePermission()
            }
        }

    private fun hasManageExternalStoragePermission() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        }else{
            return false
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

    private fun setUpTransformer(){
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        outputPlayerView?.player= null

        kotlin.run {
            mergeVideos()
        }
    }

    private fun mergeVideos() {
        progressBar.visibility = View.VISIBLE

        transformer = Transformer
            .Builder(this)
            .addListener(this@MergeVidsActivity)
            .build()

        for(url in videoUrl!!){
            editedMediaItemList.add(EditedMediaItem.Builder(MediaItem.fromUri(url)).build())
        }

        val mediaItemSequence = EditedMediaItemSequence.Builder(editedMediaItemList).build()

        val composition = Composition.Builder(listOf(mediaItemSequence)).build()



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

    private fun releasePlayer(){
        inputPlayer1?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            inputPlayer1 = null
        }

        inputPlayer2?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            inputPlayer2 = null
        }

        outputPlayer?.release()
        outputPlayer = null
    }

    private fun initOutputPlayer(){
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        outputPlayerView?.player = outputPlayer

        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        progressBar.visibility = View.GONE
        outputPlayerView?.visibility = View.VISIBLE

        editedMediaItemList.clear()
        videoUrl?.clear()
//        saveBtn.visibility = View.VISIBLE
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

    override fun onError(composition: Composition, exportResult: ExportResult,exportException: ExportException) {
        outputPlayerView?.visibility = View.GONE
        progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        inputPlayerView1?.onPause()
        inputPlayerView2?.onPause()
    }

    override fun onPause() {
        super.onPause()
        inputPlayerView1?.onPause()
        inputPlayerView2?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }


}