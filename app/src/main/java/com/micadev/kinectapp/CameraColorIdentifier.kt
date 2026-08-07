package com.micadev.kinectapp

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraColorIdentifier(
    private val context: Context,
    private val onGestureDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Limiar de escuridão. Valores do canal Y vão de 0 (Preto) a 255 (Branco).
    // Tudo abaixo de 50 é considerado "preto" (sua luva/camisa).
    private val DARK_THRESHOLD = 50 

    // Controle de tempo para evitar múltiplos cliques seguidos por frame
    private var lastGestureTime = 0L
    private val cooldownMillis = 500L // meio segundo entre um comando e outro

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // Acessamos apenas o plano 0 (Luminância/Y) para ser ultra-rápido
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val data = ByteArray(buffer.capacity())
        buffer.get(data)

        val width = image.width
        val height = image.height

        var darkPixelsCount = 0
        var sumX = 0
        var sumY = 0

        // Varremos a imagem pulando de 4 em 4 pixels para economizar processamento
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val index = y * yPlane.rowStride + x * yPlane.pixelStride
                // Converte o byte para valor positivo (0 a 255)
                val pixelValue = data[index].toInt() and 0xFF 

                if (pixelValue < DARK_THRESHOLD) {
                    darkPixelsCount++
                    sumX += x
                    sumY += y
                }
            }
        }

        // Se encontrarmos uma "mancha" preta grande o suficiente (ex: uma luva)
        if (darkPixelsCount > 100) {
            val centerX = sumX / darkPixelsCount
            val centerY = sumY / darkPixelsCount

            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastGestureTime > cooldownMillis) {
                lastGestureTime = currentTime

                // Lê qual modo está ativo nas preferências do app
                val prefs = context.getSharedPreferences("KinectPrefs", Context.MODE_PRIVATE)
                val isSwipeMode = prefs.getBoolean("is_swipe_mode", false)

                if (isSwipeMode) {
                    // Lógica para Jogos Tipo Runner (Subway Surfers - Swipes)
                    if (centerY < height / 3) {
                        onGestureDetected("PULO (Swipe Cima)")
                        TouchDispatcher.swipe(500f, 1500f, 500f, 500f)
                    } else if (centerX < width / 3) {
                        onGestureDetected("ESQUERDA (Swipe)")
                        TouchDispatcher.swipe(800f, 1000f, 200f, 1000f)
                    } else if (centerX > 2 * width / 3) {
                        onGestureDetected("DIREITA (Swipe)")
                        TouchDispatcher.swipe(200f, 1000f, 800f, 1000f)
                    }
                } else {
                    // Lógica para Jogos de Tiro (Toques Simples)
                    if (centerY < height / 3) {
                        onGestureDetected("SOCO_ALTO / TIRO")
                        val targetX = prefs.getFloat("target_x", 500f)
                        val targetY = prefs.getFloat("target_y", 800f)
                        TouchDispatcher.clickAt(targetX, targetY)
                    }
                }
            }
        }

        // É obrigatório fechar a imagem para o CameraX liberar o próximo frame
        image.close() 
    }
}
