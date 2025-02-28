package com.lokesh.media3

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lokesh.media3.databinding.ActivityVoiceOverBinding
import com.lokesh.media3.viewModel.VoiceOverViewModel

class VoiceOverActivity : AppCompatActivity() {
    private val voiceOverVM : VoiceOverViewModel by viewModels()
    private lateinit var binding : ActivityVoiceOverBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceOverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.recordBtn.setOnClickListener {
            if(voiceOverVM.isRecording.value == true)
                voiceOverVM.stopRecording()
            else
                voiceOverVM.startRecording()
        }
        
        voiceOverVM.isRecording.observe(this){isR->
            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                binding.recordBtn,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.7f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.7f)
            )
            scaleDown.duration = 70
            
            scaleDown.doOnEnd {
                binding.recordBtn.setImageResource(if (isR) R.drawable.ic_stop else R.drawable.ic_mic)
                
                val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                    binding.recordBtn,
                    PropertyValuesHolder.ofFloat("scaleX", 0.7f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.7f, 1f)
                )
                scaleUp.duration = 70
                scaleUp.start()
            }
            
            binding.startOrStopRecording.text = if(isR) "Stop Recording" else "Start Recording"
            
            scaleDown.start()
            
        }
    }
    
}