package com.micadev.kinectapp

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraColorIdentifier(
    private val context: Context,
    private val onGestureDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastGestureTime = 0L
    private val cooldownMillis = 400L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val data = ByteArray(buffer.capacity())
        buffer.get(data)

        val width = image.width
        val height = image.height

        // Lê o limiar de sensibilidade configurado na SeekBar pelo usuário (padrão: 50)
        val prefs = context.getSharedPreferences("KinectPrefs", Context.MODE_PRIVATE)
        val darkThreshold = prefs.getInt("dark_threshold", 50)
        val isSwipeMode = prefs.getBoolean("is_swipe_mode", false)

        // Listas para rastrear o centro das manchas escuras detectadas
        val darkBlobs = mutableListOf<Pair<Int, Int>>()

        // Varredura da imagem pulando de 6 em 6 pixels para máxima performance em tempo real
        for (y in 0 until height step 6) {
            for (x in 0 until width step 6) {
                val index = y * yPlane.rowStride + x * yPlane.pixelStride
                val pixelValue = data[index].toInt() and 0xFF 

                if (pixelValue < darkThreshold) {
                    darkBlobs.add(Pair(x, y))
                }
            }
        }

        // Se houver pixels escuros suficientes para rastreamento
        if (darkBlobs.size > 50) {
            // Separação em regies da câmera:
            // Tronco/Corpo: Centro da imagem
            // Luva Esquerda e Luva Direita: Laterais superiores
            
            val centerXLimit = width / 2
            val upperHeightLimit = height / 2

            var bodyPixels = 0
            var leftHandPixels = 0
            var rightHandPixels = 0

            var leftHandSumY = 0
            var rightHandSumY = 0

            for (blob in darkBlobs) {
                val bx = blob.first
                val by = blob.second

                // Identifica se é o tronco (camisa preta centralizada)
                if (bx in (width / 4)..(3 * width / 4) && by > upperHeightLimit) {
                    bodyPixels++
                } 
                // Luva Esquerda (Lado esquerdo da tela da câmera = sua mão direita)
                else if (bx < centerXLimit && by <= upperHeightLimit) {
                    leftHandPixels++
                    leftHandSumY += by
                } 
                // Luva Direita (Lado direito da tela da câmera = sua mão esquerda)
                else if (bx >= centerXLimit && by <= upperHeightLimit) {
                    rightHandPixels++
                    rightHandSumY += by
                }
            }

            // Valida se o "Corpo" (camisa) está presente para estabilizar o esqueleto
            val hasBodyDetected = bodyPixels > 80

            if (hasBodyDetected) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastGestureTime > cooldownMillis) {
                    
                    // Detecta soco da Luva Esquerda (subiu acima da média esperada)
                    if (leftHandPixels > 15) {
                        lastGestureTime = currentTime
                        if (isSwipeMode) {
                            onGestureDetected("LUVA ESQ: Swipe Esquerda")
                            TouchDispatcher.swipe(800f, 1000f, 200f, 1000f)
                        } else {
                            onGestureDetected("SOCO ESQ (Ataque)")
                            val targetX = prefs.getFloat("target_x", 300f)
                            val targetY = prefs.getFloat("target_y", 800f)
                            TouchDispatcher.clickAt(targetX, targetY)
                        }
                    } 
                    // Detecta soco da Luva Direita
                    else if (rightHandPixels > 15) {
                        lastGestureTime = currentTime
                        if (isSwipeMode) {
                            onGestureDetected("LUVA DIR: Swipe Direita")
                            TouchDispatcher.swipe(200f, 1000f, 800f, 1000f)
                        } else {
                            onGestureDetected("SOCO DIR (Ataque)")
                            val targetX = prefs.getFloat("target_x", 700f)
                            val targetY = prefs.getFloat("target_y", 800f)
                            TouchDispatcher.clickAt(targetX, targetY)
                        }
                    }
                }
            }
        }

        image.close()
    }
}
