
package com.mcqautomation.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FloatingButtonService : Service() {
    
    companion object {
        var isRunning = false
        private const val TAG = "FloatingButtonService"
    }
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var playButton: ImageView? = null
    private var settingsButton: ImageView? = null
    private var optionButtonsContainer: LinearLayout? = null
    private var optionButtons: Array<ImageView?> = arrayOfNulls(4)
    
    private lateinit var ocrHelper: OCRHelper
    private lateinit var geminiAPI: GeminiAPI
    private lateinit var autoClickService: AutoClickService
    private lateinit var prefs: SharedPreferences
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isProcessing = false
    private var showOptionButtons = false
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ocrHelper = OCRHelper(this)
        geminiAPI = GeminiAPI(this)
        autoClickService = AutoClickService()
        prefs = getSharedPreferences("mcq_settings", Context.MODE_PRIVATE)
        
        createFloatingView()
        isRunning = true
        Log.d(TAG, "FloatingButtonService created")
    }
    
    private fun createFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_buttons_layout, null)
        
        playButton = floatingView?.findViewById(R.id.play_button)
        settingsButton = floatingView?.findViewById(R.id.settings_button)
        optionButtonsContainer = floatingView?.findViewById(R.id.option_buttons_container)
        
        // Initialize option buttons
        optionButtons[0] = floatingView?.findViewById(R.id.option_a_button)
        optionButtons[1] = floatingView?.findViewById(R.id.option_b_button)
        optionButtons[2] = floatingView?.findViewById(R.id.option_c_button)
        optionButtons[3] = floatingView?.findViewById(R.id.option_d_button)
        
        setupButtonListeners()
        loadSettings()
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        windowManager.addView(floatingView, params)
        makeDraggable(floatingView!!, params)
    }
    
    private fun setupButtonListeners() {
        playButton?.setOnClickListener {
            if (!isProcessing) {
                startMCQProcess()
            }
        }
        
        settingsButton?.setOnClickListener {
            openSettingsPanel()
        }
        
        // Setup option button listeners
        for (i in optionButtons.indices) {
            optionButtons[i]?.setOnClickListener {
                val option = ('A' + i).toString()
                performOptionTap(option, i)
            }
        }
    }
    
    private fun startMCQProcess() {
        serviceScope.launch {
            try {
                isProcessing = true
                updatePlayButtonState(ButtonState.PROCESSING)
                
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "Starting MCQ process...")
                
                // Step 1: Capture screen
                val bitmap = ocrHelper.captureScreen()
                val captureTime = System.currentTimeMillis()
                Log.d(TAG, "Screen capture took: ${captureTime - startTime}ms")
                
                // Step 2: Extract text using OCR
                val extractedText = ocrHelper.extractText(bitmap)
                val ocrTime = System.currentTimeMillis()
                Log.d(TAG, "OCR took: ${ocrTime - captureTime}ms")
                
                // Step 3: Parse MCQ
                val mcqData = ocrHelper.parseMCQ(extractedText)
                if (mcqData == null) {
                    Log.e(TAG, "Failed to parse MCQ from text: $extractedText")
                    updatePlayButtonState(ButtonState.ERROR)
                    return@launch
                }
                
                // Step 4: Get answer from Gemini API or cache
                val answer = geminiAPI.getAnswer(mcqData)
                val apiTime = System.currentTimeMillis()
                Log.d(TAG, "API call took: ${apiTime - ocrTime}ms")
                
                // Step 5: Perform tap
                val optionIndex = answer[0] - 'A'
                if (showOptionButtons) {
                    // Use coordinate-based tapping
                    performOptionTap(answer, optionIndex)
                } else {
                    // Use accessibility service
                    autoClickService.performClick(answer)
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Total process time: ${totalTime}ms")
                
                updatePlayButtonState(ButtonState.SUCCESS)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in MCQ process", e)
                updatePlayButtonState(ButtonState.ERROR)
            } finally {
                isProcessing = false
            }
        }
    }
    
    private fun performOptionTap(option: String, optionIndex: Int) {
        val coordinates = getOptionCoordinates(optionIndex)
        if (coordinates != null) {
            autoClickService.performClickAt(coordinates.first, coordinates.second)
            Log.d(TAG, "Tapped option $option at coordinates: ${coordinates.first}, ${coordinates.second}")
        }
    }
    
    private fun getOptionCoordinates(optionIndex: Int): Pair<Int, Int>? {
        return when (optionIndex) {
            0 -> Pair(prefs.getInt("option_a_x", -1), prefs.getInt("option_a_y", -1))
            1 -> Pair(prefs.getInt("option_b_x", -1), prefs.getInt("option_b_y", -1))
            2 -> Pair(prefs.getInt("option_c_x", -1), prefs.getInt("option_c_y", -1))
            3 -> Pair(prefs.getInt("option_d_x", -1), prefs.getInt("option_d_y", -1))
            else -> null
        }?.takeIf { it.first != -1 && it.second != -1 }
    }
    
    private fun updatePlayButtonState(state: ButtonState) {
        playButton?.setImageDrawable(
            when (state) {
                ButtonState.IDLE -> ContextCompat.getDrawable(this, R.drawable.ic_play)
                ButtonState.PROCESSING -> ContextCompat.getDrawable(this, R.drawable.ic_loading)
                ButtonState.SUCCESS -> ContextCompat.getDrawable(this, R.drawable.ic_success)
                ButtonState.ERROR -> ContextCompat.getDrawable(this, R.drawable.ic_error)
            }
        )
    }
    
    private fun openSettingsPanel() {
        // Launch settings activity or show overlay panel
        val intent = Intent(this, SettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
    
    private fun loadSettings() {
        showOptionButtons = prefs.getBoolean("show_option_buttons", false)
        optionButtonsContainer?.visibility = if (showOptionButtons) View.VISIBLE else View.GONE
    }
    
    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        isRunning = false
        Log.d(TAG, "FloatingButtonService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    enum class ButtonState {
        IDLE, PROCESSING, SUCCESS, ERROR
    }
}
