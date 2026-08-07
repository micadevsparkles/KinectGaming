package com.micadev.kinectapp

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraColorIdentifier(private val onGestureDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    // Limiar de escuridão. Valores do canal Y vão de 0 (Preto) a 255 (Branco).
    // Tudo abaixo de 50 é considerado "preto" (sua luva/camisa).
    private val DARK_THRESHOLD = 50 

    // Controle de tempo para evitar múltiplos cliques seguidos por frame
    private var lastGestureTime = 0L
    private val cooldownMillis = 500L // meio segundo entre um soco e outro

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

            // Se o centro da mancha preta estiver na parte superior da imagem, é um Soco!
            if (centerY < height / 3) {
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastGestureTime > cooldownMillis) {
                    lastGestureTime = currentTime
                    
                    onGestureDetected("SOCO_ALTO")
                    
                    // Dispara o toque físico simulado na tela do jogo
                    // Altere os valores (500f, 800f) para a posição exata do botão no seu jogo
                    TouchDispatcher.clickAt(500f, 800f)
                }
            }
        }

        // É obrigatório fechar a imagem para o CameraX liberar o próximo frame
        image.close() 
    }
}
