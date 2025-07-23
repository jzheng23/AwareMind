package com.jianzheng.studyzero.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.HostManager
import com.jianzheng.studyzero.data.input.Input
import com.jianzheng.studyzero.data.input.InputDao
import com.jianzheng.studyzero.data.input.InputDatabase
import com.jianzheng.studyzero.data.input.toFirebaseInput
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.*
import java.time.LocalDate

class MyAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val LISTENER_INTERVAL = 300L
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val INPUT_TYPE_SCROLL = 1
        private const val INPUT_TYPE_CLICK = 2
        private const val INPUT_TYPE_TEXT = 3

        @Volatile
        private var DATABASE_INSTANCE: InputDatabase? = null

        private fun getDatabase(context: Context): InputDatabase {
            return DATABASE_INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InputDatabase::class.java,
                    "input_database"
                ).fallbackToDestructiveMigration(false).build()
                DATABASE_INSTANCE = instance
                instance
            }
        }
    }

    // Timing and state variables
    private var textChangeCount = 0
    private var isTextInputActive = false
    private var previousScrollX = 0
    private var previousScrollY = 0
    private var totalDeltaX = 0
    private var totalDeltaY = 0
    private var isScrolling = false
    private var lastScrollEventTime: Long = 0
    private var textInputStartTime: Long = 0

    // Configuration and data
    private var inputToBeRecord = false
    private var uid = ""
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    // Database and Firebase
    private lateinit var db: InputDatabase
    private lateinit var inputDao: InputDao
    private val database = Firebase.database
    private lateinit var myRef: DatabaseReference

    // Caching for optimization
    private var cachedHost: String? = null
    private var cachedDate: String? = null
    private var lastDateCheck: Long = 0

    // Coroutine management
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()

        try {
            // Initialize shared preferences
            sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
            setupPreferencesListener()

            // Initialize user ID
            uid = MyPreferenceManager.getString(this, getString(R.string.user_ID))

            // Initialize database
            db = getDatabase(this)
            inputDao = db.inputDao()

            // Initialize Firebase reference
            myRef = database.getReference("Input")

            // Cache host on startup
            cachedHost = HostManager.getHost(this)
            updateCachedDate()

            // Check initial recording state
            inputToBeRecord = sharedPrefs.getBoolean("recording_input", false)

        } catch (e: Exception) {
            Log.e(TAG, "Error during service initialization", e)
        }
    }

    private fun setupPreferencesListener() {
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "recording_input") {
                inputToBeRecord = prefs.getBoolean(key, false)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun updateCachedDate() {
        val currentTime = System.currentTimeMillis()
        if (cachedDate == null || currentTime - lastDateCheck > DAY_IN_MILLIS) {
            cachedDate = LocalDate.now().toString()
            lastDateCheck = currentTime
        }
    }

    private fun writeData(input: Input) {
        if (!inputToBeRecord) {
            return
        }

        updateCachedDate()

        serviceScope.launch {
            try {
                val generatedId = inputDao.insert(input)

                val uidValid = validateUid(uid)

                if (uidValid && cachedHost != null && cachedDate != null) {
                    val firebaseInput = input.copy(id = generatedId).toFirebaseInput()
                    val path = "$uid/$cachedHost/$cachedDate/$generatedId"

                    myRef.child(uid)
                        .child(cachedHost!!)
                        .child(cachedDate!!)
                        .child(generatedId.toString())
                        .setValue(firebaseInput)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to write to Firebase: $path", exception)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing data", e)
            }
        }
    }

    private fun createInput(startTime: Long, inputType: Int, extraX: Int, extraY: Int): Input {
        return Input(
            startTime = startTime,
            inputType = inputType,
            extraX = extraX,
            extraY = extraY
        )
    }

    private fun handleTextInputEnd(currentTime: Long) {
        if (isTextInputActive) {
            val input = createInput(
                startTime = currentTime,
                inputType = INPUT_TYPE_TEXT,
                extraX = textChangeCount,
                extraY = 0
            )
            writeData(input)
            isTextInputActive = false
            textChangeCount = 0
        }
    }

    private fun handleScrollEnd(currentTime: Long) {
        if (isScrolling) {
            val input = createInput(
                startTime = currentTime,
                inputType = INPUT_TYPE_SCROLL,
                extraX = totalDeltaX,
                extraY = totalDeltaY
            )
            writeData(input)

            // Reset scroll state
            isScrolling = false
            totalDeltaX = 0
            totalDeltaY = 0
        }
    }

    private fun handleScrollEvent(event: AccessibilityEvent, currentTime: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!isScrolling) {
                // Start of a new scroll operation
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.w(TAG, "Received null accessibility event")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Handle scroll timeout
        if ((currentTime - lastScrollEventTime > LISTENER_INTERVAL) && isScrolling) {
            handleScrollEnd(currentTime)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (!isTextInputActive) {
                    textInputStartTime = currentTime
                    textChangeCount = 1
                    isTextInputActive = true
                } else {
                    textChangeCount++
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val input = createInput(
                    startTime = currentTime,
                    inputType = INPUT_TYPE_CLICK,
                    extraX = 0,
                    extraY = 0
                )
                writeData(input)
                handleTextInputEnd(currentTime)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleTextInputEnd(currentTime)
                handleScrollEvent(event, currentTime)
            }

            else -> {}
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        // Cancel all coroutines
        serviceScope.cancel()

        // Unregister preferences listener
        if (::sharedPrefs.isInitialized) {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }

        // Close database
        if (::db.isInitialized) {
            db.close()
        }

        super.onDestroy()
    }
}