package com.jianzheng.studyzero.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.jianzheng.studyzero.tool.getDateTime

class MyAccessibilityService : AccessibilityService() {
    private val listenerInterval = 300L
    private var textChangeCount = 0
    private var isTextInputActive = false
    private var previousScrollX = 0
    private var previousScrollY = 0
    private var totalDeltaX = 0
    private var totalDeltaY = 0
    private var isScrolling = false
    private var scrollStartTime: Long = 0
    private var lastScrollEventTime: Long = 0
    private var textInputStartTime: Long = 0
    private var lastWindowsChangeTime: Long = 0
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("inputLogging", "connected")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentTime = System.currentTimeMillis()

        if ((currentTime - lastScrollEventTime > listenerInterval) and isScrolling) {
            Log.d(
                "inputLogging",
                "${getDateTime(scrollStartTime)} scroll $totalDeltaX $totalDeltaY"
            )
            isScrolling = false
            totalDeltaX = 0
            totalDeltaY = 0
        }
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (!isTextInputActive) {
                    textInputStartTime = currentTime
                    textChangeCount = 1
                    isTextInputActive = true
                } else {
                    // Continuing the series
                    textChangeCount++
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d("inputLogging", "${getDateTime(currentTime)} click")
                if (isTextInputActive) {
                    Log.d(
                        "inputLogging",
                        "${getDateTime(currentTime)} textChanged $textChangeCount"
                    )
                    isTextInputActive = false
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isTextInputActive) {
                    Log.d(
                        "inputLogging",
                        "${getDateTime(currentTime)} textChanged $textChangeCount"
                    )
                    isTextInputActive = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (!isScrolling) {
                        // Start of a new scroll operation
                        scrollStartTime = currentTime
                        totalDeltaX = event.scrollDeltaX
                        totalDeltaY = event.scrollDeltaY
                        isScrolling = true
                    } else {
                        // Continuing the scroll operation
                        totalDeltaX += event.scrollDeltaX
                        totalDeltaY += event.scrollDeltaY
                    }
                } else {
                    if (!isScrolling) {
                        // Start of a new scroll operation
                        isScrolling = true
                        totalDeltaX = 0
                        totalDeltaY = 0
                    } else {
                        // Continuing the scroll operation
                        val deltaX = event.scrollX - previousScrollX
                        val deltaY = event.scrollY - previousScrollY
                        totalDeltaX += deltaX
                        totalDeltaY += deltaY
                    }

                    // Update previous scroll positions after calculations
                    previousScrollX = event.scrollX
                    previousScrollY = event.scrollY
                }

                lastScrollEventTime = currentTime
            }

            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (currentTime - lastWindowsChangeTime > listenerInterval) {
                    Log.d("inputLogging", "${getDateTime(currentTime)} windows changed")
                }
                lastWindowsChangeTime = currentTime

            }

            else -> {}
        }

    }

    override fun onInterrupt() {
        // Handle the service being interrupted
        Log.d("inputLogging", "interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("inputLogging", "destroyed")
    }
}