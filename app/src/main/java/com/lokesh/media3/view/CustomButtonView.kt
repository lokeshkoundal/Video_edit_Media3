package com.lokesh.media3.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

private const val SQUARE_SIZE = 300

/**
@JvmOverloads is an annotation in Kotlin that helps generate multiple overloaded constructors
or methods when using Kotlin code in Java.
 
 *@param context The context in which the view is created.
 * @param attributeSet Set of attributes defined in XML.
 * @param defStyleAttr Default style attributes.
 *@author Lokesh koundal
 */
class CustomButtonView @JvmOverloads constructor (
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context,attributeSet,defStyleAttr) {
    
    private val rect = Rect()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    
    init {
        initView()
    }
    
    private fun initView() {
        paint.color = Color.GREEN
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        rect.left = 10
        rect.top = 10
        rect.right = rect.left + SQUARE_SIZE
        rect.bottom = rect.top + SQUARE_SIZE
        
        canvas.drawRect(rect,paint)
    }
    
    fun swapColor(){
        paint.color = if(paint.color == Color.GREEN) Color.RED else Color.GREEN
        postInvalidate()

    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = SQUARE_SIZE + paddingLeft + paddingRight
        val width = resolveSize(desiredSize, widthMeasureSpec)
        val height = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    
    
}