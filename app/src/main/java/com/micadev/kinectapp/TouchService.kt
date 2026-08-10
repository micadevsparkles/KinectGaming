package com.micadev.kinectapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.res.Resources
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

object TouchDispatcher {
    var service: TouchService? = null
    
    fun clickAt(x: Float, y: Float) {
        service?.simulateClick(x, y)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        service?.simulateSwipe(startX, startY, endX, endY)
    }

    // Função Nova: Swipe com base no tamanho dinâmico da tela
    fun swipeDirection(direction: String) {
        val dm = Resources.getSystem().displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f
        
        // Arrasta percorrendo 30% da tela na direção solicitada
        val distX = dm.widthPixels * 0.3f
        val distY = dm.heightPixels * 0.3f
        
        var ex = cx
        var ey = cy
        
        when (direction) {
            "CIMA" -> ey = cy - distY
            "BAIXO" -> ey = cy + distY
            "ESQUERDA" -> ex = cx - distX
            "DIREITA" -> ex = cx + distX
        }
        service?.simulateSwipe(cx, cy, ex, ey)
    }
}

class TouchService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        TouchDispatcher.service = this
    }

    fun simulateClick(x: Float, y: Float) {
        val dm = resources.displayMetrics
        
        // CoerceIn garante que o toque nunca vai apontar para fora da tela (Corrigindo o erro em jogos modo Paisagem)
        val safeX = x.coerceIn(0f, dm.widthPixels.toFloat() - 1f)
        val safeY = y.coerceIn(0f, dm.heightPixels.toFloat() - 1f)

        val path = Path().apply {
            moveTo(safeX, safeY)
        }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, 10)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        dispatchGesture(gesture, null, null)
    }

    fun simulateSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        // Duração um pouco maior (150ms) para jogos interpretarem o arraste corretamente
        val stroke = GestureDescription.StrokeDescription(path, 0, 150)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        TouchDispatcher.service = null
        return super.onUnbind(intent)
    }
}
