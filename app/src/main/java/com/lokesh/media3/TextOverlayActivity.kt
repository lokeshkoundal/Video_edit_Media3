package com.lokesh.media3

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import com.lokesh.media3.databinding.ActivityTextOverlayBinding
import java.io.File

@UnstableApi
class TextOverlayActivity : AppCompatActivity(),Transformer.Listener {
    lateinit var binding : ActivityTextOverlayBinding

    private var inputPlayer : ExoPlayer? = null
    private var outputPlayer : ExoPlayer? = null

    private var playbackPosition = 0L
    private var playWhenReady = true

    private var fileName : String? = null
    private var filePath : File? = null

    private var transformer : Transformer? = null

    private var dX = 0f
    private var dY = 0f



    private var videoUrl : String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTextOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.addTextBtn.setOnClickListener {
            setUpTransformer()
        }

        binding.addVideoBtn.setOnClickListener {
            launchNewVideoPicker()
        }

        binding.overlayTextView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }

            }
            true
        }


        binding.editText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                binding.overlayTextView.text = binding.editText.text
            }

        })

    }

    fun getNormalizedPosition(textView: TextView, playerView: PlayerView): Pair<Float, Float> {
        val xNormalized = textView.x / playerView.width // Normalize X
        val yNormalized = textView.y / playerView.height // Normalize Y
        return Pair(xNormalized, yNormalized)
    }

    private fun setUpTransformer(){
        outputPlayer?.stop()
        outputPlayer?.release()
        outputPlayer = null
        binding.outputPlayerView.player = null

        startTransforming()

    }

    private fun startTransforming() {
        val effects = ImmutableList.Builder<Effect>()
        val overlay = createOverlayEffectFromBundle()
        if(overlay!=null){
            effects.add(overlay)

            transformer = Transformer.Builder(this)
                .addListener(this)
                .build()

            val inputMediaItem  = MediaItem.Builder().apply {
                setUri(videoUrl)
            }.build()

            val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
                setEffects(Effects(mutableListOf(),effects.build()))
            }

            filePath = createExternalFile()
            filePath?.absolutePath?.let { transformer!!.start(editedMediaItem.build(), it) }
        }
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

    private fun createOverlayEffectFromBundle(): OverlayEffect? {
        binding.progressBar.visibility = View.VISIBLE

        val (xNormalized, yNormalized) = getNormalizedPosition(binding.overlayTextView, binding.inputPlayerView)

        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()


        val overlaySettings = OverlaySettings.Builder()
            .setAlphaScale(1f)
//            .setBackgroundFrameAnchor(0.7f,0.1f)
            .setOverlayFrameAnchor(xNormalized,yNormalized)
//            .setBackgroundFrameAnchor(xNormalized,yNormalized)
            .setScale(1f,1f)
            .build()


        val overlayText = SpannableString(binding.editText.text)
        overlayText.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this,R.color.black)),
            0,
            overlayText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val textOverlay = TextOverlay.createStaticTextOverlay(overlayText,overlaySettings)
        overlaysBuilder.add(textOverlay)

        val overlays = overlaysBuilder.build()

        return if(overlays.isEmpty()) null else OverlayEffect(overlays)
    }

    private fun launchNewVideoPicker(){
        newVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    private val newVideoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()){ uri->
        if(uri!=null){
            videoUrl = uri.toString()
            if(Util.SDK_INT >= 24){
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

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)

        binding.progressBar.visibility = View.GONE
        binding.outputPlayerView.visibility = View.VISIBLE

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
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this,exportException.message,Toast.LENGTH_SHORT).show()

    }
}