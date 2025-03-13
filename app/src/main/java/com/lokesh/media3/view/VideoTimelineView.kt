package com.lokesh.media3.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class VideoTimelineView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW; alpha = 150 }
    private val handleWidth = 40f
    private val minTrimWidth = 100f
    private var startX = 100f
    private var endX = 600f
    private var draggingStart = false
    private var draggingEnd = false
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val timelineHeight = height.toFloat()
        val centerY = height / 2f
        
        // Draw background rectangle
        canvas.drawRect(startX, centerY - timelineHeight / 2, endX, centerY + timelineHeight / 2, rectPaint)
        
        // Draw handles (start and end)
        canvas.drawRect(startX - handleWidth / 2, centerY - timelineHeight / 2, startX + handleWidth / 2, centerY + timelineHeight / 2, handlePaint)
        canvas.drawRect(endX - handleWidth / 2, centerY - timelineHeight / 2, endX + handleWidth / 2, centerY + timelineHeight / 2, handlePaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (event.x) {
                    in (startX - handleWidth)..(startX + handleWidth) -> {
                        draggingStart = true
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    in (endX - handleWidth)..(endX + handleWidth) -> {
                        draggingEnd = true
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    else -> {
                        parent.requestDisallowInterceptTouchEvent(false)
                        return false
                    }
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (draggingStart) {
                    startX = max(0f, min(event.x, endX - minTrimWidth))
                } else if (draggingEnd) {
                    endX = max(startX + minTrimWidth, min(event.x, width.toFloat()))
                }
                invalidate()
            }
            
            
            MotionEvent.ACTION_UP -> {
                draggingStart = false
                draggingEnd = false
                parent.requestDisallowInterceptTouchEvent(false) // Allow RecyclerView scrolling again
            }
        }
        return true
    }
}
