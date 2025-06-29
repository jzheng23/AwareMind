package com.jianzheng.studyzero.service

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.HostManager
import com.jianzheng.studyzero.data.input.Input
import com.jianzheng.studyzero.data.input.InputDao
import com.jianzheng.studyzero.data.input.InputDatabase
import com.jianzheng.studyzero.data.input.toFirebaseInput
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    //    private var lastWindowsChangeTime: Long = 0
    private var inputToBeRecord = false
    private var uid = ""
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    private lateinit var db: InputDatabase
    private lateinit var inputDao: InputDao

    private val database = Firebase.database
    private val myRef = database.getReference("Input")

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
//        inputToBeRecord = sharedPrefs.getBoolean("input_record", false)
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "recording_input") {
                inputToBeRecord = prefs.getBoolean(key, false)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        uid = MyPreferenceManager.getString(this, this.getString(R.string.user_ID))

        db = Room.databaseBuilder(this, InputDatabase::class.java, "input_database")
            .fallbackToDestructiveMigration(false)
            .build()
        inputDao = db.inputDao()
//        Log.d("inputLogging", "connected")
    }

    private fun writeData(input: Input) {
        val host = HostManager.getHost(this)
        val todayDate = LocalDate.now().toString()
        CoroutineScope(Dispatchers.IO).launch {
            val generatedId = inputDao.insert(input)
            if (validateUid(uid)) {
                myRef.child(uid)
                    .child(host)
                    .child(todayDate)
                    .child(generatedId.toString())
                    .setValue(input.copy(id = generatedId).toFirebaseInput())
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentTime = System.currentTimeMillis()


        if ((currentTime - lastScrollEventTime > listenerInterval) and isScrolling) {
//            Log.d(
//                "inputLogging",
//                "${getDateTime(scrollStartTime)} scroll $totalDeltaX $totalDeltaY"
//            )
            val input1 = Input(
                startTime = currentTime,
                inputType = 1,
                extraX = totalDeltaX,
                extraY = totalDeltaY
            )
            writeData(input1)
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
//                Log.d("inputLogging", "${getDateTime(currentTime)} click")
                val input2 = Input(
                    startTime = currentTime,
                    inputType = 2,
                    extraX = 0,
                    extraY = 0
                )
                writeData(input2)
                if (isTextInputActive) {
//                    Log.d(
//                        "inputLogging",
//                        "${getDateTime(currentTime)} textChanged $textChangeCount"
//                    )
                    val input3 = Input(
                        startTime = currentTime,
                        inputType = 3,
                        extraX = textChangeCount,
                        extraY = 0
                    )
                    writeData(input3)
                    isTextInputActive = false
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isTextInputActive) {
//                    Log.d(
//                        "inputLogging",
//                        "${getDateTime(currentTime)} textChanged $textChangeCount"
//                    )
                    val input4 = Input(
                        startTime = currentTime,
                        inputType = 3,
                        extraX = textChangeCount,
                        extraY = 0
                    )
                    writeData(input4)
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


            else -> {}
        }
    }

    override fun onInterrupt() {
        // Handle the service being interrupted
//        Log.d("inputLogging", "interrupted")
    }

    override fun onDestroy() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
//        Log.d("inputLogging", "destroyed")
        super.onDestroy()
    }
}