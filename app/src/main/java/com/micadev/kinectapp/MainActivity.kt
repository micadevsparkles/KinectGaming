package com.micadev.kinectapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micadev.kinectapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Registra o pedido de permissão da câmera
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

        binding.btnToggleService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionsAndStart()
            } else {
                binding.tvStatus.text = "Serviço Desativado"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                val serviceIntent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
            }
        }
    }

    private fun checkPermissionsAndStart() {
        // 1. Checa Sobreposição
        if (!Settings.canDrawOverlays(this)) {
            binding.btnToggleService.isChecked = false
            Toast.makeText(this, "Permita a sobreposição para ver os controles no jogo.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        // 2. Checa Câmera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            binding.btnToggleService.isChecked = false
            requestCameraLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        // Se tem as duas permissões, liga o sistema!
        binding.tvStatus.text = "Kinect Ativo (Rodando em Segundo Plano)"
        binding.tvStatus.setTextColor(android.graphics.Color.GREEN)
        
        // TODO: Iniciar o OverlayService aqui futuramente
    }
}
