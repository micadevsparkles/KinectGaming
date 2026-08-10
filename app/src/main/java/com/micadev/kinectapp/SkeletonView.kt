package com.micadev.kinectapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class SkeletonView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var landmarks: List<NormalizedLandmark> = emptyList()
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    fun setLandmarks(newLandmarks: List<NormalizedLandmark>) {
        landmarks = newLandmarks
        invalidate() // Força o re-desenho
    }

    fun clear() {
        landmarks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Desenha os pontos
        for (landmark in landmarks) {
            // Inverte o X pelo espelhamento da tela
            canvas.drawCircle((1f - landmark.x()) * w, landmark.y() * h, 8f, paint)
        }

        // Função auxiliar para desenhar a linha do osso
        fun drawBone(start: Int, end: Int) {
            canvas.drawLine(
                (1f - landmarks[start].x()) * w, landmarks[start].y() * h,
                (1f - landmarks[end].x()) * w, landmarks[end].y() * h,
                paint
            )
        }

        // Ligações básicas do Homem-Palito (MediaPipe Pose Indexes)
        drawBone(11, 12) // Ombros
        drawBone(11, 23) // Tronco Esq
        drawBone(12, 24) // Tronco Dir
        drawBone(23, 24) // Quadris
        drawBone(11, 13) // Braço Esq
        drawBone(13, 15) // Antebraço Esq
        drawBone(12, 14) // Braço Dir
        drawBone(14, 16) // Antebraço Dir
        drawBone(23, 25) // Perna Esq
        drawBone(25, 27) // Canela Esq
        drawBone(24, 26) // Perna Dir
        drawBone(26, 28) // Canela Dir
    }
}
