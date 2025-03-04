package com.lokesh.media3.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lokesh.media3.databinding.ActivityVideoEditorBinding

class VideoEditorActivity : AppCompatActivity() {
    lateinit var binding : ActivityVideoEditorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.backBtn.setOnClickListener {
            finish()
        }
        
        

    }
}