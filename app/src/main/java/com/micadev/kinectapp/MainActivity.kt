package com.micadev.kinectapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.micadev.kinectapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura o comportamento do botão grande
        binding.btnToggleService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Ao afundar o botão (ON)
                if (checkOverlayPermission()) {
                    binding.tvStatus.text = "Serviço de Sobreposição Ativo"
                    binding.tvStatus.setTextColor(android.graphics.Color.GREEN)
                    // TODO: Iniciar o Service em Background com o CameraX aqui
                } else {
                    binding.btnToggleService.isChecked = false // Volta o botão pra cima
                    requestOverlayPermission()
                }
            } else {
                // Ao soltar o botão (OFF)
                binding.tvStatus.text = "Serviço Desativado"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                // TODO: Parar o Service
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Precisamos da permissão de sobreposição!", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:\$packageName")
        )
        startActivityForResult(intent, 1000)
    }
}
