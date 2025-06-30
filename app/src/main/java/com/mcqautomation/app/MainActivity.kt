
package com.mcqautomation.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mcqautomation.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        updateStatusText()
        
        binding.enableFloatingButton.setOnClickListener {
            if (checkPermissions()) {
                startFloatingButtonService()
            } else {
                requestPermissions()
            }
        }
        
        binding.enableAccessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }
    
    private fun checkPermissions(): Boolean {
        return Settings.canDrawOverlays(this)
    }
    
    private fun requestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
    
    private fun startFloatingButtonService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        startService(serviceIntent)
        Toast.makeText(this, "Floating button enabled", Toast.LENGTH_SHORT).show()
        updateStatusText()
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun updateStatusText() {
        val status = buildString {
            append("Status:\n")
            append("• Overlay Permission: ${if (Settings.canDrawOverlays(this@MainActivity)) "✓" else "✗"}\n")
            append("• Accessibility Service: ${if (AutoClickService.isServiceEnabled(this@MainActivity)) "✓" else "✗"}\n")
            append("• Floating Button: ${if (FloatingButtonService.isRunning) "✓" else "✗"}")
        }
        binding.statusText.text = status
    }
    
    override fun onResume() {
        super.onResume()
        updateStatusText()
    }
}
