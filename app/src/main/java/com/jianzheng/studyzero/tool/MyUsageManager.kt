package com.jianzheng.studyzero.tool

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.util.Log
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.HostManager
import com.jianzheng.studyzero.data.usage.Usage
import com.jianzheng.studyzero.data.usage.UsageDatabase
import com.jianzheng.studyzero.data.usage.toFirebaseUsage
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MyUsageManager {
    companion object {
        private const val TAG = "MyUsageManager"
        private const val BATCH_SIZE = 50
        private const val MIN_SESSION_DURATION_MS = 1000L // 1 second minimum
    }

    suspend fun getUsageEvents(context: Context, currentTime: Long, test: Boolean = true) {
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.e(TAG, "Failed to get UsageStatsManager")
            return
        }

        val dayInMillis: Long = 24 * 60 * 60 * 1000L
        val dayInMillisTest: Long = 10 * 60 * 1000L
        val rangeMillis = if (test) dayInMillisTest else dayInMillis

//        var usageEvents: UsageEvents? = null
        var db: UsageDatabase? = null

        try {
            val usageEvents = usageStatsManager.queryEvents(currentTime - rangeMillis, currentTime)
            if (usageEvents == null) {
                Log.e(TAG, "Failed to query usage events")
                return
            }

            val database = Firebase.database
            val myRef = database.getReference("Usage")
            val uid = MyPreferenceManager.getString(context, context.getString(R.string.user_ID))

            db = Room.databaseBuilder(context, UsageDatabase::class.java, "usage_database")
                .fallbackToDestructiveMigration(false)
                .build()

            val usageDao = db.usageDao()
            // Keep using both packageName and className for session tracking
            val sessionStartTimes = mutableMapOf<Pair<String, String>, Long>()
            val host = HostManager.getHost(context)
            val yesterdayDate = LocalDate.now().minusDays(1).toString()

            // Process events in batches for memory efficiency
            val allBatches = mutableListOf<List<Usage>>()
            val currentBatch = mutableListOf<Usage>()
            var processedCount = 0

            while (usageEvents.hasNextEvent()) {
                try {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)

                    // Validate event data
                    if (event.packageName.isNullOrBlank() || event.className.isNullOrBlank()) {
                        continue
                    }

                    // Use both packageName and className as key for accurate session tracking
                    val sessionKey = Pair(event.packageName, event.className)

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            sessionStartTimes[sessionKey] = event.timeStamp
                        }

                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            sessionStartTimes[sessionKey]?.let { startTime ->
                                // Data validation
                                if (isValidUsageSession(startTime, event.timeStamp)) {
                                    // Create Usage object with only packageName (no className)
                                    val usage = Usage(
                                        packageName = event.packageName,
                                        // Remove className from Usage object
                                        startTime = startTime,
                                        endTime = event.timeStamp
                                    )
                                    currentBatch.add(usage)
                                    sessionStartTimes.remove(sessionKey)
                                    if (currentBatch.size >= BATCH_SIZE) {
                                        allBatches.add(currentBatch.toList())
                                        currentBatch.clear()
                                    }
                                }
                            }
                        }
                    }

                    processedCount++

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing usage event: ${e.message}")
                    continue
                }
            }

            // Process remaining items in the batch
            if (currentBatch.isNotEmpty()) {
                allBatches.add(currentBatch.toList())
            }

            withContext(Dispatchers.IO) {
                allBatches.forEach { batch ->
                    processBatch(batch, usageDao, myRef, uid, host, yesterdayDate)
                }
            }

            Log.d(TAG, "Successfully processed $processedCount usage events")

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing usage stats: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in getUsageEvents: ${e.message}")
        } finally {
            // Resource cleanup
            try {
                db?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing database: ${e.message}")
            }
        }
    }

    private fun isValidUsageSession(startTime: Long, endTime: Long): Boolean {
        return endTime > startTime &&
                (endTime - startTime) >= MIN_SESSION_DURATION_MS &&
                startTime > 0
    }

    private suspend fun processBatch(
        usages: List<Usage>,
        usageDao: com.jianzheng.studyzero.data.usage.UsageDao,
        myRef: com.google.firebase.database.DatabaseReference,
        uid: String?,
        host: String,
        yesterdayDate: String
    ) {
        // Remove CoroutineScope.launch wrapper - just process directly
        try {
            usages.forEach { usage ->
                try {
                    val generatedId = usageDao.insert(usage)

                    if (validateUid(uid) && uid != null) {
                        val firebaseUsage = usage.copy(id = generatedId).toFirebaseUsage()
                        myRef.child(uid)
                            .child(host)
                            .child(yesterdayDate)
                            .child(generatedId.toString())
                            .setValue(firebaseUsage)
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully synced usage data to Firebase: $generatedId")
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Failed to sync usage data to Firebase: ${exception.message}")
                            }
                    } else {
                        Log.w(TAG, "Skipping Firebase sync - invalid or null uid")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing individual usage record: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing usage batch: ${e.message}")
        }
    }
}