
package com.mcqautomation.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.mcqautomation.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("mcq_settings", Context.MODE_PRIVATE)
        
        setupUI()
        loadSettings()
        setupListeners()
    }
    
    private fun setupUI() {
        // Set up resolution radio buttons
        binding.resolution480p.text = "480p"
        binding.resolution720p.text = "720p (Default)"
        binding.resolution1080p.text = "1080p"
        
        // Set up region selector
        binding.regionSelectorButton.text = "Select Screen Region"
        
        // Set up option buttons toggle
        binding.showOptionButtonsSwitch.text = "Show A, B, C, D buttons"
        
        // Set up positioning buttons
        binding.positionAButton.text = "Position A"
        binding.positionBButton.text = "Position B"
        binding.positionCButton.text = "Position C"
        binding.positionDButton.text = "Position D"
    }
    
    private fun loadSettings() {
        // Load resolution setting
        val resolution = prefs.getString("resolution", "720p")
        when (resolution) {
            "480p" -> binding.resolution480p.isChecked = true
            "720p" -> binding.resolution720p.isChecked = true
            "1080p" -> binding.resolution1080p.isChecked = true
        }
        
        // Load option buttons setting
        binding.showOptionButtonsSwitch.isChecked = prefs.getBoolean("show_option_buttons", false)
        
        // Show/hide positioning buttons based on toggle
        updatePositioningButtonsVisibility()
    }
    
    private fun setupListeners() {
        // Resolution selection
        binding.resolutionGroup.setOnCheckedChangeListener { _, checkedId ->
            val resolution = when (checkedId) {
                R.id.resolution_480p -> "480p"
                R.id.resolution_720p -> "720p"
                R.id.resolution_1080p -> "1080p"
                else -> "720p"
            }
            prefs.edit().putString("resolution", resolution).apply()
        }
        
        // Region selector
        binding.regionSelectorButton.setOnClickListener {
            // Open region selector overlay
            openRegionSelector()
        }
        
        // Option buttons toggle
        binding.showOptionButtonsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_option_buttons", isChecked).apply()
            updatePositioningButtonsVisibility()
        }
        
        // Positioning buttons
        binding.positionAButton.setOnClickListener { startPositioning("A") }
        binding.positionBButton.setOnClickListener { startPositioning("B") }
        binding.positionCButton.setOnClickListener { startPositioning("C") }
        binding.positionDButton.setOnClickListener { startPositioning("D") }
        
        // Save and close
        binding.saveSettingsButton.setOnClickListener {
            finish()
        }
    }
    
    private fun updatePositioningButtonsVisibility() {
        val showButtons = binding.showOptionButtonsSwitch.isChecked
        binding.positioningButtonsContainer.visibility = if (showButtons) View.VISIBLE else View.GONE
    }
    
    private fun openRegionSelector() {
        // Start region selector overlay
        val intent = android.content.Intent(this, RegionSelectorActivity::class.java)
        startActivity(intent)
    }
    
    private fun startPositioning(option: String) {
        // Start positioning mode for the specified option
        val intent = android.content.Intent(this, PositioningActivity::class.java)
        intent.putExtra("option", option)
        startActivity(intent)
    }
}
