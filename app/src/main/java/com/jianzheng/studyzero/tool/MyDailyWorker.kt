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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.core.content.edit
import com.jianzheng.studyzero.data.HostManager

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val zoneId = ZoneId.systemDefault()
    private val host = HostManager.getHost(ctx)
    private val todayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val yesterdayStart = todayStart - ChronoUnit.DAYS.duration.toMillis()
    private val yesterdayDate = LocalDate.now().minusDays(1).toString()
    private val uid = MyPreferenceManager.getString(ctx, ctx.getString(R.string.user_ID))
    override fun doWork(): Result {
        val context: Context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            uploadUsageData(context)
            uploadResponseData(context)
            uploadInputData(context)
            uploadActivityData(context)
            uploadUnlockData(context)
            uploadUserStats(context)
            dayCountPlusOne(context)
        }
        resetTriggerList(context)
        startMyForegroundService(context)
//        Log.d("workManager", "Do work la!")
        val currentTime = System.currentTimeMillis()
        MyPreferenceManager.setString(context, "last_maintenance", getDateTime(currentTime))
        return Result.success()
    }

    private suspend fun uploadUnlockData(context: Context) {

        if (validateUid(uid)) {
            val unlockDao =
                Room.databaseBuilder(context, UnlockDatabase::class.java, "unlock_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .unlockDao()
            val myRef = Firebase.database.getReference("Unlock")
            unlockDao.getAllYesterday(yesterdayStart, todayStart)
                .forEach { entry ->
                    myRef.child(uid)
                        .child(host)
                        .child(yesterdayDate)
                        .child(entry.id.toString())
                        .setValue(entry.toFirebaseUnlock())
                }
        }
    }

    private suspend fun uploadActivityData(context: Context) {
        if (validateUid(uid)) {
            val activityDao =
                Room.databaseBuilder(context, ActivityDatabase::class.java, "activity_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .activityDao()
            val myRef = Firebase.database.getReference("Activity")
            activityDao.getAllYesterday(yesterdayStart, todayStart)
                .forEach { entry ->
                    myRef.child(uid)
                        .child(host)
                        .child(yesterdayDate)
                        .child(entry.id.toString())
                        .setValue(entry)
                }
        }
    }

    private suspend fun uploadInputData(context: Context) {
        if (validateUid(uid)) {
            val inputDao =
                Room.databaseBuilder(context, InputDatabase::class.java, "input_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .inputDao()
            val myRef = Firebase.database.getReference("Input")
            inputDao.getAllYesterday(yesterdayStart, todayStart)
                .forEach { entry ->
                    myRef.child(uid)
                        .child(host)
                        .child(yesterdayDate)
                        .child(entry.id.toString())
                        .setValue(InputNoId.fromInput(entry))
                }
        }
    }

    private suspend fun uploadResponseData(context: Context) {
        if (validateUid(uid)) {
            val responseDao =
                Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .responseDao()
            val myRef = Firebase.database.getReference("ESM")
            responseDao.getAllYesterday(yesterdayStart, todayStart)
                .forEach { entry ->
                    myRef.child(uid)
                        .child(host)
                        .child(yesterdayDate)
                        .child(entry.id.toString())
                        .setValue(ResponseNoId.fromResponse(entry))
                }
        }
    }

    private fun uploadUserStats(context: Context) {
        if (validateUid(uid)) {
            val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
            val todayCount = sharedPreferences.getInt("count_today", 0)
            val totalCount = sharedPreferences.getInt("count_total", 0)
            val dayCount = sharedPreferences.getInt("count_day", 0)
            val todayDate = LocalDate.now().toString()
            val currentTimestamp = System.currentTimeMillis()

            val userStats = mapOf(
                "timestamp" to currentTimestamp,
                "todayCount" to todayCount,
                "totalCount" to totalCount,
                "dayCount" to dayCount
            )

            val myRef = Firebase.database.getReference("user_stats")
            myRef.child(uid)
                .child(host)
                .child(todayDate)
                .setValue(userStats)
        }
    }

    private fun dayCountPlusOne(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            val dayCount = sharedPreferences.getInt("count_day", 0)
            putInt("count_day", dayCount + 1)
        }
        Log.d("workManager", "day + 1")
    }

    private fun uploadUsageData(context: Context) {
        val myUsageManager = MyUsageManager()
        myUsageManager.getUsageEvents(context, System.currentTimeMillis(), false)
    }

    private fun resetTriggerList(context: Context) {
        val responseCounter = ResponseCounter(context)
        responseCounter.resetCounter()
        MyDelayManager.resetTriggerList()
    }
}

