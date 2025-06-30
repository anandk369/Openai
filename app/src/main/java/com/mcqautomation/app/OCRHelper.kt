
package com.mcqautomation.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "OCRHelper"
        private val MCQ_PATTERN = Regex(
            """(.+?)\s*[A-D]\)\s*(.+?)\s*[A-D]\)\s*(.+?)\s*[A-D]\)\s*(.+?)\s*[A-D]\)\s*(.+?)""",
            RegexOption.DOT_MATCHES_ALL
        )
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val prefs: SharedPreferences = context.getSharedPreferences("mcq_settings", Context.MODE_PRIVATE)
    private var mediaProjection: MediaProjection? = null
    
    data class MCQData(
        val question: String,
        val options: List<String>
    )
    
    suspend fun captureScreen(): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Get screen capture region
                val region = getCaptureRegion()
                
                // For demo purposes, create a sample bitmap
                // In real implementation, use MediaProjection API
                val bitmap = createSampleBitmap(region.width(), region.height())
                
                Log.d(TAG, "Screen captured: ${bitmap.width}x${bitmap.height}")
                continuation.resume(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing screen", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    private fun createSampleBitmap(width: Int, height: Int): Bitmap {
        // Create a sample bitmap for demonstration
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    
    private fun getCaptureRegion(): Rect {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
        // Get saved region or use default (center 80% of screen)
        val x = prefs.getInt("capture_x", (screenWidth * 0.1).toInt())
        val y = prefs.getInt("capture_y", (screenHeight * 0.2).toInt())
        val width = prefs.getInt("capture_width", (screenWidth * 0.8).toInt())
        val height = prefs.getInt("capture_height", (screenHeight * 0.6).toInt())
        
        return Rect(x, y, x + width, y + height)
    }
    
    suspend fun extractText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            val processedBitmap = preprocessBitmap(bitmap)
            val image = InputImage.fromBitmap(processedBitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    Log.d(TAG, "Extracted text: $text")
                    continuation.resume(text)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Text recognition failed", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
    
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // Convert to grayscale and increase contrast
        val processed = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(processed)
        
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Grayscale
            postConcat(ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, -50f,  // Red
                0f, 1.5f, 0f, 0f, -50f,  // Green
                0f, 0f, 1.5f, 0f, -50f,  // Blue
                0f, 0f, 0f, 1f, 0f       // Alpha
            )))
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return processed
    }
    
    fun parseMCQ(text: String): MCQData? {
        try {
            // Clean the text
            val cleanText = text.trim().replace(Regex("\\s+"), " ")
            
            // Try to match the MCQ pattern
            val lines = cleanText.split("\n").filter { it.trim().isNotEmpty() }
            
            // Find question (usually the longest line without A), B), C), D) pattern)
            val question = lines.find { line ->
                !line.contains(Regex("[A-D]\\)")) && line.length > 20
            } ?: lines.firstOrNull() ?: return null
            
            // Find options
            val options = mutableListOf<String>()
            val optionPattern = Regex("([A-D])\\)\\s*(.+)")
            
            for (line in lines) {
                val match = optionPattern.find(line)
                if (match != null) {
                    options.add(match.groupValues[2].trim())
                }
            }
            
            if (options.size >= 4) {
                Log.d(TAG, "Parsed MCQ - Question: $question, Options: $options")
                return MCQData(question.trim(), options.take(4))
            }
            
            // Fallback: try to extract from raw text
            return fallbackMCQParsing(cleanText)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MCQ", e)
            return null
        }
    }
    
    private fun fallbackMCQParsing(text: String): MCQData? {
        // Simple fallback - split by common patterns
        val parts = text.split(Regex("(?=[A-D]\\))"))
        if (parts.size >= 5) {
            val question = parts[0].trim()
            val options = parts.drop(1).take(4).map { part ->
                part.replace(Regex("^[A-D]\\)\\s*"), "").trim()
            }
            
            if (question.isNotEmpty() && options.all { it.isNotEmpty() }) {
                return MCQData(question, options)
            }
        }
        return null
    }
}
