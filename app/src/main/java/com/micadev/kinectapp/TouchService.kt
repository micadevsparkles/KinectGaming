package com.micadev.kinectapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.content.res.Resources

object TouchDispatcher {
    var service: TouchService? = null
    var targetX: Float = 500f
    var targetY: Float = 500f

    fun executeAction(action: String) {
        if (service == null) return
        val x = targetX
        val y = targetY
        val offset = 300f

        when (action) {
            "CLICK" -> service?.click(x, y)
            "DUPLO_CLICK" -> { service?.click(x, y, isDouble = true) }
            "LONG_PRESS" -> service?.longPress(x, y)
            "SWIPE_UP" -> service?.swipe(x, y, x, y - offset)
            "SWIPE_DOWN" -> service?.swipe(x, y, x, y + offset)
            "SWIPE_LEFT" -> service?.swipe(x, y, x - offset, y)
            "SWIPE_RIGHT" -> service?.swipe(x, y, x + offset, y)
            "SWIPE_UP_LEFT" -> service?.swipe(x, y, x - offset, y - offset)
            "SWIPE_UP_RIGHT" -> service?.swipe(x, y, x + offset, y - offset)
            "SWIPE_DOWN_LEFT" -> service?.swipe(x, y, x - offset, y + offset)
            "SWIPE_DOWN_RIGHT" -> service?.swipe(x, y, x + offset, y + offset)
            "PINCA" -> service?.pinch(x, y)
        }
    }
}

class TouchService : AccessibilityService() {
    private var isExecuting = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        TouchDispatcher.service = this
    }

    private fun dispatchSafeGesture(gesture: GestureDescription) {
        if (isExecuting) return
        isExecuting = true
        
        try {
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { isExecuting = false }
                override fun onCancelled(gestureDescription: GestureDescription?) { isExecuting = false }
            }, null)
        } catch (e: Exception) {
            isExecuting = false
        }
    }

    fun click(x: Float, y: Float, isDouble: Boolean = false) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val builder = GestureDescription.Builder().addStroke(stroke)
        
        if (isDouble) {
            val stroke2 = GestureDescription.StrokeDescription(path, 150, 100)
            builder.addStroke(stroke2)
        }
        
        dispatchSafeGesture(builder.build())
    }

    fun longPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchSafeGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 3000)).build())
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        dispatchSafeGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build())
    }

    fun pinch(x: Float, y: Float) {
        val path1 = Path().apply { moveTo(x - 200, y); lineTo(x, y) }
        val path2 = Path().apply { moveTo(x + 200, y); lineTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path1, 0, 400)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0, 400)
        dispatchSafeGesture(GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
