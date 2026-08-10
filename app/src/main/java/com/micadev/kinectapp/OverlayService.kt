package com.micadev.kinectapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.drawable.GradientDrawable

class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var cameraView: View
    private lateinit var targetView: View // Nova Janela para o Alvo
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupTargetCircle()
        setupCameraWindow()
        
        cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun setupTargetCircle() {
        // Cria visualmente o círculo tracejado via código
        targetView = android.widget.FrameLayout(this).apply {
            val circle = android.view.View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(10, android.graphics.Color.BLACK, 20f, 10f) // Borda tracejada
                    setColor(android.graphics.Color.TRANSPARENT)
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(150, 150)
            }
            addView(circle)
        }

        val params = WindowManager.LayoutParams(
            150, 150,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = android.view.Gravity.CENTER

        // Arrastar o Alvo
        var initX = 0; var initY = 0; var initTouchX = 0f; var initTouchY = 0f
        targetView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    initTouchX = event.rawX; initTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (event.rawX - initTouchX).toInt()
                    params.y = initY + (event.rawY - initTouchY).toInt()
                    windowManager.updateViewLayout(targetView, params)
                    
                    // Atualiza a posição do alvo globalmente para os toques!
                    val location = IntArray(2)
                    targetView.getLocationOnScreen(location)
                    TouchDispatcher.targetX = location[0] + 75f // Centro
                    TouchDispatcher.targetY = location[1] + 75f
                    true
                }
                else -> false
            }
        }
        windowManager.addView(targetView, params)
    }

    private fun setupCameraWindow() {
        cameraView = android.view.LayoutInflater.from(this).inflate(R.layout.layout_floating_camera, null)
        val params = WindowManager.LayoutParams(240, 320, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT)
        params.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        
        // Adicione aqui a mesma lógica de arrastar (setOnTouchListener) que já tínhamos para a câmeraView...
        
        windowManager.addView(cameraView, params)
    }

    private fun startCamera() {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(cameraView.findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val skeletonView = cameraView.findViewById<SkeletonView>(R.id.skeletonView)

            val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also { analysis ->
                    analysis.setAnalyzer(cameraExecutor, MediaPipePoseTracker(this, skeletonView) { actionText ->
                        // Atualiza o texto na UI
                    })
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(cameraView)
        windowManager.removeView(targetView)
    }
}
