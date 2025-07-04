package com.jianzheng.studyzero.tool

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.activity.ActivityDatabase
import com.jianzheng.studyzero.data.input.InputDatabase
import com.jianzheng.studyzero.data.input.InputNoId
import com.jianzheng.studyzero.data.response.ResponseDatabase
import com.jianzheng.studyzero.data.response.ResponseNoId
import com.jianzheng.studyzero.data.unlock.UnlockDatabase
import com.jianzheng.studyzero.data.unlock.toFirebaseUnlock
import com.jianzheng.studyzero.ui.validateUid
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.core.content.edit
import com.jianzheng.studyzero.data.HostManager
// **NEW IMPORTS**
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    // **EXTRACTED CONSTANTS**
    companion object {
        private const val TAG = "MyDailyWorker"
        private const val SHARED_PREFS_NAME = "MySharedPrefs"
        private const val PREF_COUNT_TODAY = "count_today"
        private const val PREF_COUNT_TOTAL = "count_total"
        private const val PREF_COUNT_DAY = "count_day"
        private const val PREF_LAST_MAINTENANCE = "last_maintenance"

        private const val FIREBASE_UNLOCK_REF = "Unlock"
        private const val FIREBASE_ACTIVITY_REF = "Activity"
        private const val FIREBASE_INPUT_REF = "Input"
        private const val FIREBASE_ESM_REF = "ESM"
        private const val FIREBASE_USER_STATS_REF = "user_stats"
    }

    private val zoneId = ZoneId.systemDefault()
    private val host = HostManager.getHost(ctx)
    private val todayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val yesterdayStart = todayStart - ChronoUnit.DAYS.duration.toMillis()
    private val yesterdayDate = LocalDate.now().minusDays(1).toString()
    private val uid = MyPreferenceManager.getString(ctx, ctx.getString(R.string.user_ID))

    override fun doWork(): Result {
        return try {
            // **EARLY VALIDATION**
            if (!validateUid(uid)) {
                Log.w(TAG, "Invalid UID, skipping daily worker execution")
                return Result.success()
            }

            val context: Context = applicationContext

            // **ROBUST ERROR HANDLING WITH PARALLEL PROCESSING**
            runBlocking {
                try {
                    // Run usage data upload first (it seems to have its own database handling)
                    uploadUsageDataSafely(context)

                    // Then run the batch upload and user stats in parallel
                    val uploadJobs = listOf(
                        async { uploadAllDataBatch(context) },
                        async { uploadUserStatsSafely(context) }
                    )
                    uploadJobs.awaitAll()

                    // These must run after uploads complete
                    dayCountPlusOne(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during data upload operations", e)
                    throw e // Re-throw to trigger retry
                }
            }

            // **SAFER EXECUTION OF CLEANUP TASKS**
            try {
                resetTriggerList(context)
                startMyForegroundService(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup operations", e)
                // Don't fail the worker for cleanup errors
            }

            val currentTime = System.currentTimeMillis()
            MyPreferenceManager.setString(context, PREF_LAST_MAINTENANCE, getDateTime(currentTime))

            Log.d(TAG, "Daily worker completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Daily worker failed", e)
            Result.retry() // **RETRY FAILED DAILY TASKS**
        }
    }

    // **BATCH FIREBASE OPERATIONS** - Upload all data in batches
    private suspend fun uploadAllDataBatch(context: Context) {
        val updates = mutableMapOf<String, Any>()

        try {
            // Collect all data into batch updates
            collectUnlockData(context, updates)
            collectActivityData(context, updates)
            collectInputData(context, updates)
            collectResponseData(context, updates)

            // **SINGLE FIREBASE WRITE** for all data
            if (updates.isNotEmpty()) {
                Firebase.database.reference.updateChildren(updates)
                Log.d(TAG, "Uploaded ${updates.size} data entries in batch")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch upload", e)
            throw e
        }
    }

    // **REFACTORED DATA COLLECTION METHODS**
    private suspend fun collectUnlockData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val unlockDao = Room.databaseBuilder(context, UnlockDatabase::class.java, "unlock_database")
                .fallbackToDestructiveMigration(false)
                .build()
                .unlockDao()

            val entries = unlockDao.getAllYesterday(yesterdayStart, todayStart)
            entries.forEach { entry ->
                val path = "$FIREBASE_UNLOCK_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = entry.toFirebaseUnlock()
            }
            Log.d(TAG, "Collected ${entries.size} unlock entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting unlock data", e)
            throw e
        }
    }

    private suspend fun collectActivityData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val activityDao = Room.databaseBuilder(context, ActivityDatabase::class.java, "activity_database")
                .fallbackToDestructiveMigration(false)
                .build()
                .activityDao()

            val entries = activityDao.getAllYesterday(yesterdayStart, todayStart)
            entries.forEach { entry ->
                val path = "$FIREBASE_ACTIVITY_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = entry
            }
            Log.d(TAG, "Collected ${entries.size} activity entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting activity data", e)
            throw e
        }
    }

    private suspend fun collectInputData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val inputDao = Room.databaseBuilder(context, InputDatabase::class.java, "input_database")
                .fallbackToDestructiveMigration(false)
                .build()
                .inputDao()

            val entries = inputDao.getAllYesterday(yesterdayStart, todayStart)
            entries.forEach { entry ->
                val path = "$FIREBASE_INPUT_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = InputNoId.fromInput(entry)
            }
            Log.d(TAG, "Collected ${entries.size} input entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting input data", e)
            throw e
        }
    }

    private suspend fun collectResponseData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val responseDao = Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
                .fallbackToDestructiveMigration(false)
                .build()
                .responseDao()

            val entries = responseDao.getAllYesterday(yesterdayStart, todayStart)
            entries.forEach { entry ->
                val path = "$FIREBASE_ESM_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = ResponseNoId.fromResponse(entry)
            }
            Log.d(TAG, "Collected ${entries.size} response entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting response data", e)
            throw e
        }
    }

    // **SAFER WRAPPER METHODS**
    private fun uploadUserStatsSafely(context: Context) {
        try {
            uploadUserStats(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading user stats", e)
            throw e
        }
    }

    private suspend fun uploadUsageDataSafely(context: Context) {
        try {
            // Add timeout to prevent hanging
            val myUsageManager = MyUsageManager()
            myUsageManager.getUsageEvents(context, System.currentTimeMillis(), false)
            Log.d(TAG, "Usage data upload completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading usage data", e)
            throw e
        }
    }

    // **UPDATED EXISTING METHODS WITH BETTER ERROR HANDLING**
    private fun uploadUserStats(context: Context) {
        try {
            val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val todayCount = sharedPreferences.getInt(PREF_COUNT_TODAY, 0)
            val totalCount = sharedPreferences.getInt(PREF_COUNT_TOTAL, 0)
            val dayCount = sharedPreferences.getInt(PREF_COUNT_DAY, 0)
            val todayDate = LocalDate.now().toString()
            val currentTimestamp = System.currentTimeMillis()

            val userStats = mapOf(
                "timestamp" to currentTimestamp,
                "todayCount" to todayCount,
                "totalCount" to totalCount,
                "dayCount" to dayCount
            )

            val myRef = Firebase.database.getReference(FIREBASE_USER_STATS_REF)
            myRef.child(uid)
                .child(host)
                .child(todayDate)
                .setValue(userStats)

            Log.d(TAG, "User stats uploaded successfully: $userStats ")

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading user stats", e)
            throw e
        }
    }

    private fun dayCountPlusOne(context: Context) {
        try {
            val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit {
                val dayCount = sharedPreferences.getInt(PREF_COUNT_DAY, 0)
                putInt(PREF_COUNT_DAY, dayCount + 1)
            }
            Log.d(TAG, "Day count incremented successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing day count", e)
            throw e
        }
    }


    private fun resetTriggerList(context: Context) {
        try {
            val responseCounter = ResponseCounter(context)
            responseCounter.resetCounter()
            MyDelayManager.resetTriggerList()
            Log.d(TAG, "Trigger list reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting trigger list", e)
            // Don't re-throw - this shouldn't fail the entire worker
        }
    }
}