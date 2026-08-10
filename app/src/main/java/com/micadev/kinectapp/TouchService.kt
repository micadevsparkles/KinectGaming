// TouchService.kt
package com.micadev.kinectapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.content.res.Resources

object TouchDispatcher {
    var service: TouchService? = null
    
    // Posição global do círculo tracejado
    var targetX: Float = 500f
    var targetY: Float = 500f

    fun executeAction(action: String) {
        if (service == null) return
        val x = targetX
        val y = targetY
        val dm = Resources.getSystem().displayMetrics
        val offset = 300f // Tamanho do swipe

        when (action) {
            "CLICK" -> service?.click(x, y)
            "DUPLO_CLICK" -> { service?.click(x, y); service?.click(x, y) }
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
    override fun onServiceConnected() {
        super.onServiceConnected()
        TouchDispatcher.service = this
    }

    fun click(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build(), null, null)
    }

    fun longPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        // 3000ms = 3 segundos
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 3000)).build(), null, null)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 200)).build(), null, null)
    }

    fun pinch(x: Float, y: Float) {
        // Movimento de dois dedos se juntando no centro (x,y)
        val path1 = Path().apply { moveTo(x - 200, y); lineTo(x, y) }
        val path2 = Path().apply { moveTo(x + 200, y); lineTo(x, y) }
        
        val stroke1 = GestureDescription.StrokeDescription(path1, 0, 300)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0, 300)
        
        val gesture = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
