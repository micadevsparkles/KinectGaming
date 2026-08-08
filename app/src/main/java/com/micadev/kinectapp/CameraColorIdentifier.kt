package com.micadev.kinectapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraColorIdentifier(
    private val context: Context,
    private val onGestureDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastGestureTime = 0L
    private val cooldownMillis = 500L
    private var currentSwipeState = "CENTRO"

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // Planos da imagem (Y = Brilho, U = Diferença de Azul, V = Diferença de Vermelho)
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val width = image.width
        val height = image.height

        val prefs = context.getSharedPreferences("KinectPrefs", Context.MODE_PRIVATE)
        val isSwipeMode = prefs.getBoolean("is_swipe_mode", false)

        var bluePixelsCount = 0
        var sumX = 0
        var sumY = 0

        for (y in 0 until height step 6) {
            for (x in 0 until width step 6) {
                val uIndex = (y / 2) * uPlane.rowStride + (x / 2) * uPlane.pixelStride
                val vIndex = (y / 2) * vPlane.rowStride + (x / 2) * vPlane.pixelStride

                val uVal = uPlane.buffer.get(uIndex).toInt() and 0xFF
                val vVal = vPlane.buffer.get(vIndex).toInt() and 0xFF

                // Rastreamento da cor Azul: alto U (azul) e baixo V (vermelho)
                if (uVal > 150 && vVal < 130) {
                    bluePixelsCount++
                    sumX += x
                    sumY += y
                }
            }
        }

        if (bluePixelsCount > 20) {
            val cx = sumX / bluePixelsCount
            val cy = sumY / bluePixelsCount

            // Divisão da tela da câmera com área neutra (deadzone) no centro (35% a 65%)
            val leftBound = width * 0.35f
            val rightBound = width * 0.65f
            val topBound = height * 0.35f
            val bottomBound = height * 0.65f

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastGestureTime > cooldownMillis) {
                if (isSwipeMode) {
                    var detectedDirection = "CENTRO"

                    if (cy < topBound) detectedDirection = "CIMA"
                    else if (cy > bottomBound) detectedDirection = "BAIXO"
                    else if (cx < leftBound) detectedDirection = "DIREITA" // Invertido por causa do espelhamento da frontal
                    else if (cx > rightBound) detectedDirection = "ESQUERDA"

                    if (detectedDirection != "CENTRO" && detectedDirection != currentSwipeState) {
                        lastGestureTime = currentTime
                        currentSwipeState = detectedDirection
                        onGestureDetected("AZUL: Swipe $detectedDirection")
                        TouchDispatcher.swipeDirection(detectedDirection)
                    } else if (detectedDirection == "CENTRO") {
                        currentSwipeState = "CENTRO" 
                    }
                } else {
                    // Modo Soco: Se a cor azul sair do centro para os lados, executa o clique
                    if (cx < leftBound || cx > rightBound) {
                        lastGestureTime = currentTime
                        onGestureDetected("AZUL: Clique Registrado!")
                        
                        // Busca o tamanho real da tela para adaptar perfeitamente ao modo paisagem
                        val screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
                        val screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()
                        
                        val targetX = prefs.getFloat("target_x", screenWidth * 0.8f)
                        val targetY = prefs.getFloat("target_y", screenHeight * 0.8f)
                        
                        TouchDispatcher.clickAt(targetX, targetY)
                    }
                }
            }
        }
        image.close()
    }
}
