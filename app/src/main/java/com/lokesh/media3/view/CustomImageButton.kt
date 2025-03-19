package com.lokesh.media3.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.lokesh.media3.R

/**
 * A custom image button view that displays an image and text.
 *
 * This class allows you to create a button-like view with an image on top and text below it.
 * It supports customization of the text, text size, text color, and the image source.
 *
 * @constructor Creates a new CustomImageButton.
 * @param context The application context.
 * @param attrs The attribute set containing custom attributes for this view.
 * @param defStyleAttr The default style attribute for this view.
 */
class CustomImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0) : View(context, attrs,defStyleAttr) {
    
    private var text: String = "Button"
    private var textSize: Float = 50f
    private var textColor: Int = Color.BLACK
    private var bitmap: Bitmap? = null
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomButtonView,
            0, 0
        )
        
        try {
            text = typedArray.getString(R.styleable.CustomButtonView_text) ?: "Button"
            textSize = typedArray.getDimension(R.styleable.CustomButtonView_textSize, 50f)
            textColor = typedArray.getColor(R.styleable.CustomButtonView_textColor, Color.BLACK)
            
            val drawableId = typedArray.getResourceId(R.styleable.CustomButtonView_imageSrc, -1)
            if (drawableId != -1) {
                bitmap = BitmapFactory.decodeResource(resources, drawableId)
                bitmap = bitmap?.let { scaleBitmap(it, 300, 400) } // Scale image to 200x200 px
            }
        } finally {
            typedArray.recycle()
        }
        
        textPaint.color = textColor
        textPaint.textSize = textSize
    }
    
    private fun scaleBitmap(original: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val aspectRatio = original.width.toFloat() / original.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (aspectRatio > 1) { // Landscape
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else { // Portrait or Square
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val topPadding = 20f
        
        bitmap?.let {
            val imageWidth = it.width.toFloat()
            val imageHeight = it.height.toFloat()
            val imageX = centerX - (imageWidth / 2)
            val imageY = topPadding
            
            canvas.drawBitmap(it, imageX, imageY, null)
            
            val textY = imageY + imageHeight + textSize + 10
            canvas.drawText(text, centerX, textY, textPaint)
        }
    }
}
