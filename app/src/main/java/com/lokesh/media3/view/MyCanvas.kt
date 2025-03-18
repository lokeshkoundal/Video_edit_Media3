package com.lokesh.media3.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class MyCanvas @JvmOverloads constructor(
    context : Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context,attributeSet,defStyleAttr) {
    
    var xPos : Float = 0f
    var yPos : Float = 0f
    
    var xPos1 : Float = 100f
    var yPos1 : Float = 200f
    var xPos2 : Float = 0f
    var yPos2 : Float = 0f
    
    var point = 1
    val rect : Rect = Rect().apply {
        left = 0
        top = 100
        right = 200
        bottom = 300
    }
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 10f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
//        canvas.drawCircle(xPos,yPos,100f,paint)
        
        if(point ==2){
            canvas.drawLine(xPos1,yPos1,xPos2,yPos2,paint)
        }else{
            canvas.drawCircle(xPos2,yPos2,6f,paint)
        }
 
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        
//        xPos = event!!.x
//        yPos = event.y
        
        if(point == 1){
            xPos1 = event!!.x
            yPos1 = event.y
            point = 2
        }else{
            xPos2 = event!!.x
            yPos2 = event.y
            point = 1
        }
        invalidate()
        return super.onTouchEvent(event)
    }

}