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

        val prefs = context.getSharedPreferences("KinectPrefs", Context.MODE_PRIVATE)
        val darkThreshold = prefs.getInt("dark_threshold", 50)
        val isSwipeMode = prefs.getBoolean("is_swipe_mode", false)

        val darkBlobs = mutableListOf<Pair<Int, Int>>()

        for (y in 0 until height step 6) {
            for (x in 0 until width step 6) {
                val index = y * yPlane.rowStride + x * yPlane.pixelStride
                val pixelValue = data[index].toInt() and 0xFF 

                if (pixelValue < darkThreshold) {
                    darkBlobs.add(Pair(x, y))
                }
            }
        }

        if (darkBlobs.size > 50) {
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

                if (bx in (width / 4)..(3 * width / 4) && by > upperHeightLimit) {
                    bodyPixels++
                } 
                else if (bx < centerXLimit && by <= upperHeightLimit) {
                    leftHandPixels++
                    leftHandSumY += by
                } 
                else if (bx >= centerXLimit && by <= upperHeightLimit) {
                    rightHandPixels++
                    rightHandSumY += by
                }
            }

            val hasBodyDetected = bodyPixels > 80

            if (hasBodyDetected) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastGestureTime > cooldownMillis) {
                    
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
