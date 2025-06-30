
package com.mcqautomation.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AutoClickService"
        private var instance: AutoClickService? = null
        
        fun isServiceEnabled(context: Context): Boolean {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            )
            
            if (accessibilityEnabled == 1) {
                val services = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                return services?.contains(context.packageName) == true
            }
            return false
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickService connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AutoClickService interrupted")
    }
    
    fun performClick(option: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        
        try {
            // Find the option to click
            val targetNode = findOptionNode(rootNode, option)
            if (targetNode != null) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked option $option using accessibility service")
                return true
            } else {
                Log.w(TAG, "Could not find option $option on screen")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
            return false
        } finally {
            rootNode.recycle()
        }
    }
    
    fun performClickAt(x: Int, y: Int): Boolean {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        
        val gesture = gestureBuilder.build()
        
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Click gesture completed at ($x, $y)")
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Click gesture cancelled at ($x, $y)")
            }
        }, null)
    }
    
    private fun findOptionNode(node: AccessibilityNodeInfo, option: String): AccessibilityNodeInfo? {
        // Look for text containing the option letter
        val text = node.text?.toString()
        if (text != null) {
            // Check if this node contains the option (e.g., "A)", "A.", "A:")
            if (text.contains("$option)") || text.contains("$option.") || text.contains("$option:")) {
                return node
            }
            
            // Also check for just the option letter at the beginning
            if (text.trim().startsWith(option)) {
                return node
            }
        }
        
        // Search child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findOptionNode(child, option)
                if (result != null) {
                    return result
                }
                child.recycle()
            }
        }
        
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "AutoClickService destroyed")
    }
}
