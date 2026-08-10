package com.micadev.kinectapp

import android.content.Context
import android.content.res.Resources
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class MediaPipeHandTracker(
    private val context: Context,
    private val onGestureDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private var lastGestureTime = 0L
    private val cooldownMillis = 500L
    private var currentSwipeState = "CENTRO"

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()
        
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // toBitmap() requer formato RGBA_8888 configurado no ImageAnalysis.Builder
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        
        val result: HandLandmarkerResult? = handLandmarker?.detect(mpImage)
        
        result?.let {
            if (it.landmarks().isNotEmpty()) {
                // Captura o Dedo Indicador (Landmark 8)
                val indexFinger = it.landmarks()[0][8]
                
                // Coordenadas normalizadas (0.0 a 1.0). Inversão no X por causa do espelhamento da câmera frontal.
                val cx = 1.0f - indexFinger.x()
                val cy = indexFinger.y()

                val prefs = context.getSharedPreferences("KinectPrefs", Context.MODE_PRIVATE)
                val isSwipeMode = prefs.getBoolean("is_swipe_mode", false)

                val leftBound = 0.35f
                val rightBound = 0.65f
                val topBound = 0.35f
                val bottomBound = 0.65f

                val currentTime = SystemClock.uptimeMillis()

                if (currentTime - lastGestureTime > cooldownMillis) {
                    if (isSwipeMode) {
                        var detectedDirection = "CENTRO"

                        if (cy < topBound) detectedDirection = "CIMA"
                        else if (cy > bottomBound) detectedDirection = "BAIXO"
                        else if (cx < leftBound) detectedDirection = "ESQUERDA"
                        else if (cx > rightBound) detectedDirection = "DIREITA"

                        if (detectedDirection != "CENTRO" && detectedDirection != currentSwipeState) {
                            lastGestureTime = currentTime
                            currentSwipeState = detectedDirection
                            onGestureDetected("MÃO: Swipe $detectedDirection")
                            TouchDispatcher.swipeDirection(detectedDirection)
                        } else if (detectedDirection == "CENTRO") {
                            currentSwipeState = "CENTRO"
                        }
                    } else {
                        // MODO SOCO
                        if (cx < leftBound || cx > rightBound) {
                            lastGestureTime = currentTime
                            onGestureDetected("MÃO: Clique Detectado!")
                            
                            val screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
                            val screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()
                            
                            val targetX = prefs.getFloat("target_x", screenWidth * 0.8f)
                            val targetY = prefs.getFloat("target_y", screenHeight * 0.8f)
                            
                            TouchDispatcher.clickAt(targetX, targetY)
                        }
                    }
                }
            }
        }
        imageProxy.close()
    }
}
