package com.micadev.kinectapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
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
    private val cooldown = 800L // Intervalo entre detecções de gestos
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        var originalBitmap: Bitmap? = null
        var fixedBitmap: Bitmap? = null

        try {
            originalBitmap = imageProxy.toBitmap()

            // 1. Rotação natural da câmera
            val cameraRotationDegrees = imageProxy.imageInfo.rotationDegrees

            // 2. Rotação da tela atual (Gira se o jogo for Paisagem/Horizontal)
            val displayRotation = windowManager.defaultDisplay.rotation
            val screenRotationDegrees = when (displayRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            // Calcula a rotação exata para alinhar a imagem com o topo da TELA
            val finalRotation = (cameraRotationDegrees - screenRotationDegrees + 360) % 360

            val matrix = Matrix().apply {
                postRotate(finalRotation.toFloat())
                postScale(-1f, 1f) // Espelhamento horizontal (Câmera frontal)
            }

            fixedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, false
            )

            val result = poseLandmarker?.detect(BitmapImageBuilder(fixedBitmap).build())

            result?.let { poseResult ->
                if (poseResult.landmarks().isNotEmpty()) {
                    val landmarks = poseResult.landmarks()[0]
                    skeletonView.setLandmarks(landmarks)

                    val nose = landmarks[0]
                    val leftHip = landmarks[23]
                    val rightHip = landmarks[24]
                    val leftAnkle = landmarks[27]
                    val rightAnkle = landmarks[28]
                    val leftWrist = landmarks[15]
                    val rightWrist = landmarks[16]

                    val now = SystemClock.uptimeMillis()
                    if (now - lastActionTime > cooldown) {
                        val prefs = context.getSharedPreferences("KinectMappings", Context.MODE_PRIVATE)
                        var detectedPhysicalAction = "NONE"

                        // Com a imagem alinhada com a orientação da TELA:
                        // Y: 0.0 (topo da tela) -> 1.0 (base da tela)
                        // X: 0.0 (esquerda) -> 1.0 (direita)

                        // Pulo: Nariz/Cabeça sobe próximo ao topo (Y < 0.22)
                        if (nose.y() < 0.22f) {
                            detectedPhysicalAction = "PULO"
                        }
                        // Agachar: Quadril desce em direção ao chão (Y > 0.75)
                        else if (leftHip.y() > 0.75f && rightHip.y() > 0.75f) {
                            detectedPhysicalAction = "AGACHAR"
                        }
                        // Andar/Correr para a Esquerda
                        else if (nose.x() < 0.32f) {
                            detectedPhysicalAction = "CORRER_ESQUERDA"
                        }
                        // Andar/Correr para a Direita
                        else if (nose.x() > 0.68f) {
                            detectedPhysicalAction = "CORRER_DIREITA"
                        }
                        // Soco: Levantar pulso
                        else if (leftWrist.y() < 0.40f || rightWrist.y() < 0.40f) {
                            detectedPhysicalAction = "SOCO"
                        }
                        // Chute: Levantar perna
                        else if (leftAnkle.y() < 0.60f || rightAnkle.y() < 0.60f) {
                            detectedPhysicalAction = "CHUTE"
                        }

                        if (detectedPhysicalAction != "NONE") {
                            val touchAction = prefs.getString(detectedPhysicalAction, "NONE")
                            if (touchAction != null && touchAction != "NONE") {
                                lastActionTime = now
                                onActionTriggered("$detectedPhysicalAction -> $touchAction")
                                TouchDispatcher.executeAction(touchAction)
                            }
                        }
                    }
                } else {
                    skeletonView.clear()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // LIMPEZA OBRIGATÓRIA DA MEMÓRIA RAM (Impede que o Android feche o app)
            originalBitmap?.recycle()
            fixedBitmap?.recycle()
            imageProxy.close()
        }
    }
}
