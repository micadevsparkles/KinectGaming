package com.micadev.kinectapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micadev.kinectapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestCameraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkPermissionsAndStart()
        } else {
            Toast.makeText(this, "A câmera é necessária para o rastreamento!", Toast.LENGTH_SHORT).show()
            binding.btnToggleService.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("KinectPrefs", MODE_PRIVATE)

        // Configura o Switch/Toggle do Serviço
        binding.btnToggleService.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (isChecked) {
                if (checkPermissionsAndStart()) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
            } else {
                binding.tvStatus.text = "Serviço Desativado"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                stopService(serviceIntent)
            }
        }

        // Seletor de Modo (Clique vs Swipe)
        binding.rgGameMode.setOnCheckedChangeListener { _, checkedId ->
            val isSwipeMode = (checkedId == R.id.rbModeSwipe)
            prefs.edit().putBoolean("is_swipe_mode", isSwipeMode).apply()
        }

        // Configuração da SeekBar de Sensibilidade (DARK_THRESHOLD)
        // Valor padrão salvo: 50 (escala de 10 a 150)
        val savedThreshold = prefs.getInt("dark_threshold", 50)
        binding.seekBarSensitivity?.progress = savedThreshold

        binding.seekBarSensitivity?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Garante um valor mínimo seguro para não bugar
                val validProgress = if (progress < 10) 10 else progress
                prefs.edit().putInt("dark_threshold", validProgress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissionsAndStart(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            binding.btnToggleService.isChecked = false
            Toast.makeText(this, "Permita a sobreposição para ver os controles no jogo.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            binding.btnToggleService.isChecked = false
            requestCameraLauncher.launch(Manifest.permission.CAMERA)
            return false
        }

        binding.tvStatus.text = "Kinect Ativo (Rodando em Segundo Plano)"
        binding.tvStatus.setTextColor(android.graphics.Color.GREEN)
        return true
    }
}
