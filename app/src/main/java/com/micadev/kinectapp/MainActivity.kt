package com.micadev.kinectapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micadev.kinectapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Listas dos movimentos e toques que você pediu
    private val physicalActions = arrayOf("NENHUM", "PULO", "AGACHAR", "CORRER_FRENTE", "CORRER_DIREITA", "CORRER_ESQUERDA", "SOCO", "CHUTE")
    private val touchActions = arrayOf("NENHUM", "CLICK", "DUPLO_CLICK", "LONG_PRESS", "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT", "SWIPE_UP_LEFT", "SWIPE_UP_RIGHT", "SWIPE_DOWN_LEFT", "SWIPE_DOWN_RIGHT", "PINCA")

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

        // Atenção: O MediaPipePoseTracker busca as configs no arquivo "KinectMappings"
        val prefs = getSharedPreferences("KinectMappings", MODE_PRIVATE)

        setupSpinners(prefs)

        binding.btnToggleService.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (isChecked) {
                if (checkPermissionsAndStart()) {
                    saveMappings(prefs) // Salva todas as combinações dos slots
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
            } else {
                binding.tvStatus.text = "Serviço Desativado"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                stopService(serviceIntent)
            }
        }
    }

    private fun setupSpinners(prefs: android.content.SharedPreferences) {
        val physAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, physicalActions)
        val touchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, touchActions)

        val physSpinners = listOf(binding.spinPhys1, binding.spinPhys2, binding.spinPhys3, binding.spinPhys4, binding.spinPhys5, binding.spinPhys6)
        val touchSpinners = listOf(binding.spinTouch1, binding.spinTouch2, binding.spinTouch3, binding.spinTouch4, binding.spinTouch5, binding.spinTouch6)

        // Preenche as opções e recupera o último estado salvo
        for (i in physSpinners.indices) {
            physSpinners[i].adapter = physAdapter
            touchSpinners[i].adapter = touchAdapter

            physSpinners[i].setSelection(prefs.getInt("ui_phys_slot_$i", 0))
            touchSpinners[i].setSelection(prefs.getInt("ui_touch_slot_$i", 0))
        }
    }

    private fun saveMappings(prefs: android.content.SharedPreferences) {
        val editor = prefs.edit()
        
        // Limpa mapeamentos antigos para não sobrepor configurações erradas
        editor.clear() 

        val physSpinners = listOf(binding.spinPhys1, binding.spinPhys2, binding.spinPhys3, binding.spinPhys4, binding.spinPhys5, binding.spinPhys6)
        val touchSpinners = listOf(binding.spinTouch1, binding.spinTouch2, binding.spinTouch3, binding.spinTouch4, binding.spinTouch5, binding.spinTouch6)

        for (i in physSpinners.indices) {
            val physIndex = physSpinners[i].selectedItemPosition
            val touchIndex = touchSpinners[i].selectedItemPosition
            
            // Salva o índice para manter os spinners na mesma posição da próxima vez que abrir o app
            editor.putInt("ui_phys_slot_$i", physIndex)
            editor.putInt("ui_touch_slot_$i", touchIndex)

            val physVal = physicalActions[physIndex]
            val touchVal = touchActions[touchIndex]

            // Cria o de-para (Ex: "PULO" recebe o valor "CLICK")
            if (physVal != "NENHUM" && touchVal != "NENHUM") {
                editor.putString(physVal, touchVal)
            }
        }
        editor.apply()
    }

    private fun checkPermissionsAndStart(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            binding.btnToggleService.isChecked = false
            Toast.makeText(this, "Permita a sobreposição para ver o alvo.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return false
        }

        if (!isAccessibilityServiceEnabled()) {
            binding.btnToggleService.isChecked = false
            Toast.makeText(this, "Ative o Kinect App nas Configurações de Acessibilidade!", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            return false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            binding.btnToggleService.isChecked = false
            requestCameraLauncher.launch(Manifest.permission.CAMERA)
            return false
        }

        binding.tvStatus.text = "Kinect Ativo (Procurando corpo...)"
        binding.tvStatus.setTextColor(android.graphics.Color.GREEN)
        return true
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val serviceId = service.resolveInfo.serviceInfo
            if (serviceId.packageName == packageName && serviceId.name == TouchService::class.java.name) {
                return true
            }
        }
        return false
    }
}
