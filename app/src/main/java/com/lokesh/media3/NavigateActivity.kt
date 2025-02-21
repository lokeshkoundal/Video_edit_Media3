package com.lokesh.media3

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.lokesh.media3.databinding.ActivityNavigateBinding

class NavigateActivity : AppCompatActivity() {
    private lateinit var binding : ActivityNavigateBinding

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.trimAudioBtn.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)

        }

        binding.mergeBtn.setOnClickListener {
            val intent = Intent(this,MergeVidsActivity::class.java)
            startActivity(intent)

        }

        binding.adjustVolumeBtn.setOnClickListener {
            val intent = Intent(this, VolumeAdjustActivity::class.java)
            startActivity(intent)

        }

    }
}