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
            Log.d(TAG, "Getting database instance")
            return DATABASE_INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InputDatabase::class.java,
                    "input_database"
                ).fallbackToDestructiveMigration(false).build()
                DATABASE_INSTANCE = instance
                Log.d(TAG, "Database instance created successfully")
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
        Log.d(TAG, "Service connected - initializing...")

        try {
            // Initialize shared preferences
            sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
            Log.d(TAG, "SharedPreferences initialized")
            setupPreferencesListener()

            // Initialize user ID
            uid = MyPreferenceManager.getString(this, getString(R.string.user_ID))
            Log.d(TAG, "User ID initialized: ${if (uid.isNotEmpty()) "***${uid.takeLast(4)}" else "EMPTY"}")

            // Initialize database
            db = getDatabase(this)
            inputDao = db.inputDao()
            Log.d(TAG, "Database and DAO initialized")

            // Initialize Firebase reference
            myRef = database.getReference("Input")
            Log.d(TAG, "Firebase reference initialized")

            // Cache host on startup
            cachedHost = HostManager.getHost(this)
            Log.d(TAG, "Cached host: $cachedHost")
            updateCachedDate()

            // Check initial recording state
            inputToBeRecord = sharedPrefs.getBoolean("recording_input", false)
            Log.d(TAG, "Initial recording state: $inputToBeRecord")

            Log.d(TAG, "Service initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during service initialization", e)
        }
    }

    private fun setupPreferencesListener() {
        Log.d(TAG, "Setting up preferences listener")
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            Log.d(TAG, "Preference changed: $key")
            if (key == "recording_input") {
                inputToBeRecord = prefs.getBoolean(key, false)
                Log.d(TAG, "Recording input preference changed to: $inputToBeRecord")
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        Log.d(TAG, "Preferences listener registered")
    }

    private fun updateCachedDate() {
        val currentTime = System.currentTimeMillis()
        if (cachedDate == null || currentTime - lastDateCheck > DAY_IN_MILLIS) {
            val oldDate = cachedDate
            cachedDate = LocalDate.now().toString()
            lastDateCheck = currentTime
            Log.d(TAG, "Date cache updated: $oldDate -> $cachedDate")
        }
    }

    private fun writeData(input: Input) {
        Log.d(TAG, "writeData called - inputToBeRecord: $inputToBeRecord")
        if (!inputToBeRecord) {
            Log.d(TAG, "Recording disabled, skipping data write")
            return
        }

        updateCachedDate()
        Log.d(TAG, "Writing input data: type=${input.inputType}, extraX=${input.extraX}, extraY=${input.extraY}")

        serviceScope.launch {
            try {
                val generatedId = inputDao.insert(input)
                Log.d(TAG, "Input inserted to local DB with ID: $generatedId")

                val uidValid = validateUid(uid)
                Log.d(TAG, "UID validation: $uidValid, cachedHost: $cachedHost, cachedDate: $cachedDate")

                if (uidValid && cachedHost != null && cachedDate != null) {
                    val firebaseInput = input.copy(id = generatedId).toFirebaseInput()
                    val path = "$uid/$cachedHost/$cachedDate/$generatedId"
                    Log.d(TAG, "Writing to Firebase path: $path")

                    myRef.child(uid)
                        .child(cachedHost!!)
                        .child(cachedDate!!)
                        .child(generatedId.toString())
                        .setValue(firebaseInput)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully wrote to Firebase: $path")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to write to Firebase: $path", exception)
                        }
                } else {
                    Log.w(TAG, "Skipping Firebase write - validation failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing data", e)
            }
        }
    }

    private fun createInput(startTime: Long, inputType: Int, extraX: Int, extraY: Int): Input {
        Log.d(TAG, "Creating input: type=$inputType, extraX=$extraX, extraY=$extraY, startTime=$startTime")
        return Input(
            startTime = startTime,
            inputType = inputType,
            extraX = extraX,
            extraY = extraY
        )
    }

    private fun handleTextInputEnd(currentTime: Long) {
        Log.d(TAG, "handleTextInputEnd called - isTextInputActive: $isTextInputActive")
        if (isTextInputActive) {
            Log.d(TAG, "Ending text input session - textChangeCount: $textChangeCount")
            val input = createInput(
                startTime = currentTime,
                inputType = INPUT_TYPE_TEXT,
                extraX = textChangeCount,
                extraY = 0
            )
            writeData(input)
            isTextInputActive = false
            textChangeCount = 0
            Log.d(TAG, "Text input session ended and reset")
        }
    }

    private fun handleScrollEnd(currentTime: Long) {
        Log.d(TAG, "handleScrollEnd called - isScrolling: $isScrolling")
        if (isScrolling) {
            Log.d(TAG, "Ending scroll session - totalDeltaX: $totalDeltaX, totalDeltaY: $totalDeltaY")
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
            Log.d(TAG, "Scroll session ended and reset")
        }
    }

    private fun handleScrollEvent(event: AccessibilityEvent, currentTime: Long) {
        Log.d(TAG, "handleScrollEvent called - Build.VERSION.SDK_INT: ${Build.VERSION.SDK_INT}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "Using API 28+ scroll handling - scrollDeltaX: ${event.scrollDeltaX}, scrollDeltaY: ${event.scrollDeltaY}")
            if (!isScrolling) {
                // Start of a new scroll operation
                totalDeltaX = event.scrollDeltaX
                totalDeltaY = event.scrollDeltaY
                isScrolling = true
                Log.d(TAG, "Started new scroll session (API 28+)")
            } else {
                // Continuing the scroll operation
                totalDeltaX += event.scrollDeltaX
                totalDeltaY += event.scrollDeltaY
                Log.d(TAG, "Continuing scroll session (API 28+) - total: ($totalDeltaX, $totalDeltaY)")
            }
        } else {
            Log.d(TAG, "Using legacy scroll handling - scrollX: ${event.scrollX}, scrollY: ${event.scrollY}")
            if (!isScrolling) {
                // Start of a new scroll operation
                isScrolling = true
                totalDeltaX = 0
                totalDeltaY = 0
                Log.d(TAG, "Started new scroll session (legacy)")
            } else {
                // Continuing the scroll operation
                val deltaX = event.scrollX - previousScrollX
                val deltaY = event.scrollY - previousScrollY
                totalDeltaX += deltaX
                totalDeltaY += deltaY
                Log.d(TAG, "Continuing scroll session (legacy) - delta: ($deltaX, $deltaY), total: ($totalDeltaX, $totalDeltaY)")
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
        val eventTypeString = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "CLICKED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLLED"
            else -> "OTHER(${event.eventType})"
        }

        Log.d(TAG, "onAccessibilityEvent: $eventTypeString from ${event.packageName}")

        // Handle scroll timeout
        if ((currentTime - lastScrollEventTime > LISTENER_INTERVAL) && isScrolling) {
            Log.d(TAG, "Scroll timeout detected - ending scroll session")
            handleScrollEnd(currentTime)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                Log.d(TAG, "Processing text change event")
                if (!isTextInputActive) {
                    textInputStartTime = currentTime
                    textChangeCount = 1
                    isTextInputActive = true
                    Log.d(TAG, "Started new text input session")
                } else {
                    textChangeCount++
                    Log.d(TAG, "Text change count incremented to: $textChangeCount")
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "Processing click event")
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
                Log.d(TAG, "Processing scroll event")
                handleTextInputEnd(currentTime)
                handleScrollEvent(event, currentTime)
            }

            else -> {
                Log.d(TAG, "Ignoring event type: ${event.eventType}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying...")

        // Cancel all coroutines
        serviceScope.cancel()
        Log.d(TAG, "Coroutines cancelled")

        // Unregister preferences listener
        if (::sharedPrefs.isInitialized) {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            Log.d(TAG, "Preferences listener unregistered")
        }

        // Close database
        if (::db.isInitialized) {
            db.close()
            Log.d(TAG, "Database closed")
        }

        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}