package com.jianzheng.studyzero.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    private var lastCheckTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("inputLogging","connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentTime = System.currentTimeMillis()
        val eventTypeString: String = AccessibilityEvent.eventTypeToString(event?.eventType ?: 0)
        if (currentTime - lastCheckTime > 1_000L) {
            lastCheckTime = currentTime
            Log.d("inputLogging","onAccessibilityEvent: $eventTypeString")
        }
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