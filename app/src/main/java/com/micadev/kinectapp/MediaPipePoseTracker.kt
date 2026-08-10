// MediaPipePoseTracker.kt
package com.micadev.kinectapp

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class MediaPipePoseTracker(
    private val context: Context,
    private val skeletonView: SkeletonView,
    private val onActionTriggered: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var poseLandmarker: PoseLandmarker? = null
    private var lastActionTime = 0L
    private val cooldown = 800L // Evita spam de ações

    init {
        val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions).setRunningMode(RunningMode.IMAGE).build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val result = poseLandmarker?.detect(BitmapImageBuilder(bitmap).build())

        result?.let { poseResult ->
            if (poseResult.landmarks().isNotEmpty()) {
                val landmarks = poseResult.landmarks()[0]
                
                // Envia os pontos para desenhar o "Homem-Palito"
                skeletonView.setLandmarks(landmarks)

                // Lógica de Gestos
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

                    // Lógicas simplificadas (Y = 0 é o topo da tela, Y = 1 é a base)
                    // Pulo: O nariz sobe rapidamente (exemplo: y < 0.2)
                    if (nose.y() < 0.25f) detectedPhysicalAction = "PULO"
                    
                    // Agachar: Quadril desce perto da base da câmera (exemplo: y > 0.8)
                    else if (leftHip.y() > 0.75f && rightHip.y() > 0.75f) detectedPhysicalAction = "AGACHAR"
                    
                    // Soco: Pulso passa a frente do corpo na lateral ou sobe na altura do rosto
                    else if (leftWrist.y() < 0.4f || rightWrist.y() < 0.4f) detectedPhysicalAction = "SOCO"
                    
                    // Chute: Tornozelo sobe alto em relação ao quadril
                    else if (leftAnkle.y() < 0.6f || rightAnkle.y() < 0.6f) detectedPhysicalAction = "CHUTE"
                    
                    // Correr Lados: Nariz ou quadris estão muito na borda
                    else if (nose.x() < 0.3f) detectedPhysicalAction = "CORRER_ESQUERDA" // Lembrete: câmera inverte E/D
                    else if (nose.x() > 0.7f) detectedPhysicalAction = "CORRER_DIREITA"

                    if (detectedPhysicalAction != "NONE") {
                        // Busca o que fazer na tela com base no mapeamento do usuário
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
