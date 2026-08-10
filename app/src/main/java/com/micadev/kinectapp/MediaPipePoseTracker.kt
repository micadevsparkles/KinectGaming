package com.micadev.kinectapp

import android.content.Context
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

class MediaPipePoseTracker(
    private val context: Context,
    private val skeletonView: SkeletonView,
    private val onActionTriggered: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var poseLandmarker: PoseLandmarker? = null
    private var lastActionTime = 0L
    private val cooldown = 1000L // Aumentado para 1 segundo para maior estabilidade

    init {
        val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions).setRunningMode(RunningMode.IMAGE).build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val originalBitmap = imageProxy.toBitmap()
        
        // CORREÇÃO DE ROTAÇÃO E ESPELHAMENTO
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) // Põe em pé
        matrix.postScale(-1f, 1f) // Efeito espelho (Câmera Frontal)
        
        val fixedBitmap = android.graphics.Bitmap.createBitmap(
            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, false
        )

        // Agora a IA analisa a imagem consertada
        val result = poseLandmarker?.detect(BitmapImageBuilder(fixedBitmap).build())

        result?.let { poseResult ->
            if (poseResult.landmarks().isNotEmpty()) {
                val landmarks = poseResult.landmarks()[0]
                
                skeletonView.setLandmarks(landmarks)

                val leftWrist = landmarks[15]
                val rightWrist = landmarks[16]
                val leftAnkle = landmarks[27]
                val rightAnkle = landmarks[28]
                val leftHip = landmarks[23]
                val rightHip = landmarks[24]
                val nose = landmarks[0]

                val now = SystemClock.uptimeMillis()
                if (now - lastActionTime > cooldown) {
                    val prefs = context.getSharedPreferences("KinectMappings", Context.MODE_PRIVATE)
                    var detectedPhysicalAction = "NONE"

                    // Pulo
                    if (nose.y() < 0.25f) detectedPhysicalAction = "PULO"
                    // Agachar
                    else if (leftHip.y() > 0.75f && rightHip.y() > 0.75f) detectedPhysicalAction = "AGACHAR"
                    // Soco
                    else if (leftWrist.y() < 0.4f || rightWrist.y() < 0.4f) detectedPhysicalAction = "SOCO"
                    // Chute
                    else if (leftAnkle.y() < 0.6f || rightAnkle.y() < 0.6f) detectedPhysicalAction = "CHUTE"
                    // Correr pra Frente
                    else if ((leftAnkle.y() < 0.75f && rightAnkle.y() > 0.85f) || (leftAnkle.y() > 0.85f && rightAnkle.y() < 0.75f)) detectedPhysicalAction = "CORRER_FRENTE"
                    // Correr Lados
                    else if (nose.x() < 0.3f) detectedPhysicalAction = "CORRER_ESQUERDA" 
                    else if (nose.x() > 0.7f) detectedPhysicalAction = "CORRER_DIREITA"

                    if (detectedPhysicalAction != "NONE") {
                        val touchAction = prefs.getString(detectedPhysicalAction, "NONE")
                        if (touchAction != "NONE") {
                            lastActionTime = now
                            onActionTriggered("$detectedPhysicalAction -> $touchAction")
                            TouchDispatcher.executeAction(touchAction!!)
                        }
                    }
                }
            } else {
                skeletonView.clear()
            }
        }
        imageProxy.close()
    }
}
