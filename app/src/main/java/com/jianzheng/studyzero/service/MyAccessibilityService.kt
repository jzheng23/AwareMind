package com.jianzheng.studyzero.service

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.jianzheng.studyzero.tool.getDateTime

class MyAccessibilityService : AccessibilityService() {
    private var lastCheckTime = 0L
    private val listenerInterval = 100L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("inputLogging","connected")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentTime = System.currentTimeMillis()
        var extraX = 0
        var extraY = 0
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {

        }
        Log.d("inputLogging","${getDateTime(currentTime)} ${event?.eventType}")
        if (currentTime - lastCheckTime > listenerInterval) {
            lastCheckTime = currentTime

        }

    }

    @Deprecated("Deprecated in Java")
    override fun onGesture(gestureId: Int): Boolean {
        when (gestureId) {
            GESTURE_SWIPE_DOWN -> {
                // Handle the swipe down gesture
                Log.d("onGesture","swipe down")
                return true
            }
            // Handle other gestures if necessary
        }
        return super.onGesture(gestureId)
    }

    override fun onInterrupt() {
        // Handle the service being interrupted
        Log.d("inputLogging","interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("inputLogging","destroyed")
    }
}