package com.jianzheng.studyzero.tool

// **NEW IMPORTS**
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.HostManager
import com.jianzheng.studyzero.data.activity.ActivityDatabase
import com.jianzheng.studyzero.data.input.InputDatabase
import com.jianzheng.studyzero.data.input.InputNoId
import com.jianzheng.studyzero.data.response.ResponseDatabase
import com.jianzheng.studyzero.data.response.ResponseNoId
import com.jianzheng.studyzero.data.response.ResponseStressDatabase
import com.jianzheng.studyzero.data.response.ResponseStressNoId
import com.jianzheng.studyzero.data.response.toDismissedEsm
import com.jianzheng.studyzero.data.unlock.UnlockDatabase
import com.jianzheng.studyzero.data.unlock.toFirebaseUnlock
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    private val twoDaysAgoStart = todayStart - ChronoUnit.DAYS.duration.toMillis() * 2
    private val threeDaysAgoStart = todayStart - ChronoUnit.DAYS.duration.toMillis() * 3
    private val yesterdayDate = LocalDate.now().minusDays(1).toString()
    private val twoDaysAgoDate = LocalDate.now().minusDays(2).toString()
    private val threeDaysAgoDate = LocalDate.now().minusDays(3).toString()
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
            collectAllDismissedResponseData(context, updates)
            collectResponseStressData(context, updates)
            collectAllDismissedResponseStressData(context, updates)

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
            val unlockDao =
                Room.databaseBuilder(context, UnlockDatabase::class.java, "unlock_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .unlockDao()

            // Get yesterday's data
            val yesterdayEntries = unlockDao.getAllYesterday(yesterdayStart, todayStart)
            yesterdayEntries.forEach { entry ->
                val path = "$FIREBASE_UNLOCK_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = entry.toFirebaseUnlock()
            }

            // Get day before yesterday's data
            val twoDaysAgoEntries = unlockDao.getAllYesterday(twoDaysAgoStart, yesterdayStart)
            twoDaysAgoEntries.forEach { entry ->
                val path = "$FIREBASE_UNLOCK_REF/$uid/$host/$twoDaysAgoDate/${entry.id}"
                updates[path] = entry.toFirebaseUnlock()
            }

            Log.d(TAG, "Collected ${yesterdayEntries.size + twoDaysAgoEntries.size} unlock entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting unlock data", e)
            throw e
        }
    }

    private suspend fun collectActivityData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val activityDao =
                Room.databaseBuilder(context, ActivityDatabase::class.java, "activity_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .activityDao()

            // Get yesterday's data
            val yesterdayEntries = activityDao.getAllYesterday(yesterdayStart, todayStart)
            yesterdayEntries.forEach { entry ->
                val path = "$FIREBASE_ACTIVITY_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = entry
            }

            // Get day before yesterday's data
            val twoDaysAgoEntries = activityDao.getAllYesterday(twoDaysAgoStart, yesterdayStart)
            twoDaysAgoEntries.forEach { entry ->
                val path = "$FIREBASE_ACTIVITY_REF/$uid/$host/$twoDaysAgoDate/${entry.id}"
                updates[path] = entry
            }

            Log.d(
                TAG,
                "Collected ${yesterdayEntries.size + twoDaysAgoEntries.size} activity entries"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting activity data", e)
            throw e
        }
    }

    private suspend fun collectResponseData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val responseDao =
                Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .responseDao()

            // Get yesterday's data
            val yesterdayEntries = responseDao.getAllYesterday(yesterdayStart, todayStart)
            yesterdayEntries.forEach { entry ->
                val path = "$FIREBASE_ESM_REF/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = ResponseNoId.fromResponse(entry)
            }

            // Get day before yesterday's data
            val twoDaysAgoEntries = responseDao.getAllYesterday(twoDaysAgoStart, yesterdayStart)
            twoDaysAgoEntries.forEach { entry ->
                val path = "$FIREBASE_ESM_REF/$uid/$host/$twoDaysAgoDate/${entry.id}"
                updates[path] = ResponseNoId.fromResponse(entry)
            }

            // Get three days ago data
            val threeDaysAgoEntries =
                responseDao.getAllYesterday(threeDaysAgoStart, twoDaysAgoStart)
            threeDaysAgoEntries.forEach { entry ->
                val path = "$FIREBASE_ESM_REF/$uid/$host/$threeDaysAgoDate/${entry.id}"
                updates[path] = ResponseNoId.fromResponse(entry)
            }

            Log.d(
                TAG,
                "Collected ${yesterdayEntries.size + twoDaysAgoEntries.size + threeDaysAgoEntries.size} response entries"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting response data", e)
            throw e
        }
    }

    private suspend fun collectAllDismissedResponseData(
        context: Context,
        updates: MutableMap<String, Any>
    ) {
        try {
            val responseDao =
                Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .responseDao()

            // Get ALL dismissed responses from the entire table
            val allDismissedResponses = responseDao.getAllOnce()
                .filter { !it.isSubmitted && it.dismissalType != null }

            allDismissedResponses.forEach { entry ->
                // Convert timestamp to date string
                val responseDate = java.time.Instant.ofEpochMilli(entry.submittingTime)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toString()
                val path = "ESM_dismissed/$uid/$host/$responseDate/${entry.id}"
                updates[path] = entry.toDismissedEsm()
            }

            Log.d(TAG, "Collected ${allDismissedResponses.size} dismissed response entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all dismissed response data", e)
            throw e
        }
    }

    private suspend fun collectResponseStressData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val responseStressDao =
                Room.databaseBuilder(context, ResponseStressDatabase::class.java, "response_stress_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .responseStressDao()

            val yesterdayEntries = responseStressDao.getAllYesterday(yesterdayStart, todayStart)
            yesterdayEntries.forEach { entry ->
                val path = "ESM_stress/$uid/$host/$yesterdayDate/${entry.id}"
                updates[path] = ResponseStressNoId.fromResponseStress(entry)
            }

            val twoDaysAgoEntries = responseStressDao.getAllYesterday(twoDaysAgoStart, yesterdayStart)
            twoDaysAgoEntries.forEach { entry ->
                val path = "ESM_stress/$uid/$host/$twoDaysAgoDate/${entry.id}"
                updates[path] = ResponseStressNoId.fromResponseStress(entry)
            }

            val threeDaysAgoEntries =
                responseStressDao.getAllYesterday(threeDaysAgoStart, twoDaysAgoStart)
            threeDaysAgoEntries.forEach { entry ->
                val path = "ESM_stress/$uid/$host/$threeDaysAgoDate/${entry.id}"
                updates[path] = ResponseStressNoId.fromResponseStress(entry)
            }

            Log.d(
                TAG,
                "Collected ${yesterdayEntries.size + twoDaysAgoEntries.size + threeDaysAgoEntries.size} stress response entries"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting stress response data", e)
            throw e
        }
    }

    private suspend fun collectAllDismissedResponseStressData(
        context: Context,
        updates: MutableMap<String, Any>
    ) {
        try {
            val responseStressDao =
                Room.databaseBuilder(context, ResponseStressDatabase::class.java, "response_stress_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .responseStressDao()

            val allDismissedResponses = responseStressDao.getAllOnce()
                .filter { !it.isSubmitted && it.dismissalType != null }

            allDismissedResponses.forEach { entry ->
                val responseDate = java.time.Instant.ofEpochMilli(entry.submittingTime)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toString()
                val path = "ESM_stress_dismissed/$uid/$host/$responseDate/${entry.id}"
                updates[path] = entry.toDismissedEsm()
            }

            Log.d(TAG, "Collected ${allDismissedResponses.size} dismissed stress response entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all dismissed stress response data", e)
            throw e
        }
    }

    private suspend fun collectInputData(context: Context, updates: MutableMap<String, Any>) {
        try {
            val inputDao =
                Room.databaseBuilder(context, InputDatabase::class.java, "input_database")
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
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
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
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
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