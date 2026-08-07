package com.micadev.kinectapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

// Esta é a "Ponte" global. Qualquer parte do app pode chamar TouchDispatcher.clickAt(x, y)
object TouchDispatcher {
    var service: TouchService? = null
    
    fun clickAt(x: Float, y: Float) {
        service?.simulateClick(x, y)
    }
}

class TouchService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Quando o serviço inicia, ele se "conecta" à nossa ponte
        TouchDispatcher.service = this
    }

    fun simulateClick(x: Float, y: Float) {
        // Cria o caminho do toque (apenas um ponto exato)
        val path = Path().apply {
            moveTo(x, y)
        }
        
        // Define a duração do toque (10 milissegundos = um toque muito rápido)
        val stroke = GestureDescription.StrokeDescription(path, 0, 10)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        // Dispara o toque no sistema!
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        TouchDispatcher.service = null
        return super.onUnbind(intent)
    }
    fun simulateSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
    val path = Path().apply {
        moveTo(startX, startY)
        lineTo(endX, endY)
    }
    
    // Duração de 100ms para parecer um gesto natural de dedo
    val stroke = GestureDescription.StrokeDescription(path, 0, 100)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    
    dispatchGesture(gesture, null, null)
}
}
