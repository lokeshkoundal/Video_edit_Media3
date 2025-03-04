package com.lokesh.media3.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoClip(
    val uri: Uri?,
    val duration: Long, // Duration in milliseconds
    val thumbnails: List<Bitmap>, // Frames for display
    val isAddButton: Boolean = false // If it's a "+" button
)