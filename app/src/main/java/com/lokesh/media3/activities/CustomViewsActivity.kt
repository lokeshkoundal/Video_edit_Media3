package com.lokesh.media3.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.lokesh.media3.R
import com.lokesh.media3.view.CustomButtonView

class CustomViewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_views)
        
        val  customButtonView = findViewById<CustomButtonView>(R.id.customButtonView)
        val swapButton = findViewById<Button>(R.id.swapBtn)
        
        swapButton.setOnClickListener {
            customButtonView.swapColor()
        }
        
    }
}