package com.lokesh.media3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.lokesh.media3.databinding.ActivityNavigateBinding

class NavigateActivity : AppCompatActivity(),OnClickListener {
    private lateinit var binding : ActivityNavigateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.trimAudioBtn.setOnClickListener(this)
        binding.mergeBtn.setOnClickListener(this)
        binding.adjustVolumeBtn.setOnClickListener(this)
        binding.mergeMusicBtn.setOnClickListener(this)
        binding.addTextBtn.setOnClickListener(this)
        binding.splitVideoBtn.setOnClickListener(this)
        binding.effectsBtn.setOnClickListener(this)
        binding.imageToVideoBtn.setOnClickListener(this)

    }

    @OptIn(UnstableApi::class)
    override fun onClick(btn: View?) {
        when(btn?.id){
            binding.trimAudioBtn.id -> {
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
            }

            binding.mergeBtn.id -> {
                val intent = Intent(this,MergeVidsActivity::class.java)
                startActivity(intent)
            }

            binding.adjustVolumeBtn.id -> {
                val intent = Intent(this, VolumeAdjustActivity::class.java)
                startActivity(intent)
            }

            binding.mergeMusicBtn.id -> {
                val intent = Intent(this, AddMusicActivity::class.java)
                startActivity(intent)
            }

            binding.addTextBtn.id -> {
                val intent = Intent(this, TextOverlayActivity::class.java)
                startActivity(intent)
            }
            
            binding.splitVideoBtn.id -> {
                val intent = Intent(this, SplitVideoActivity::class.java)
                startActivity(intent)
            }
            
            binding.effectsBtn.id -> {
                val intent = Intent(this, EffectsActivity::class.java)
                startActivity(intent)
            }
            
            binding.imageToVideoBtn.id -> {
                val intent = Intent(this, ImageToVideoActivity::class.java)
                startActivity(intent)
            }
            
            else -> {}

        }
    }
}