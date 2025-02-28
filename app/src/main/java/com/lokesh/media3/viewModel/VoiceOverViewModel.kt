package com.lokesh.media3.viewModel

import android.app.Application
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

class VoiceOverViewModel(application: Application) : AndroidViewModel(application) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> get() = _isRecording
    
    
    fun startRecording() {
        _isRecording.value = true
        
    }
    
    fun stopRecording() {
        _isRecording.value = false
        
    }
}
