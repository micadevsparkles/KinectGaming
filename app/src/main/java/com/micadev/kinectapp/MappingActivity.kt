package com.micadev.kinectapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.micadev.kinectapp.databinding.ActivityMappingBinding

class MappingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMappingBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMappingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lógica para arrastar o alvo
        binding.ivTarget.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_MOVE) {
                view.x = event.rawX - view.width / 2
                view.y = event.rawY - view.height / 2
            }
            true
        }

        binding.btnSaveMapping.setOnClickListener {
            // Salva a posição final
            val prefs = getSharedPreferences("KinectPrefs", MODE_PRIVATE)
            prefs.edit()
                .putFloat("target_x", binding.ivTarget.x + binding.ivTarget.width / 2)
                .putFloat("target_y", binding.ivTarget.y + binding.ivTarget.height / 2)
                .apply()
            finish() // Fecha e volta
        }
    }
}
