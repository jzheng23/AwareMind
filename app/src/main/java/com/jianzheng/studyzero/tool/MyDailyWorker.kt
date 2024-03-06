package com.jianzheng.studyzero.tool

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.response.ResponseDatabase
import com.jianzheng.studyzero.data.response.ResponseNoId
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val context: Context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            uploadUsageData(context)
            uploadResponsesData(context)
            dayCountPlusOne(context)
        }
        resetTriggerList(context)
        startMyForegroundService(context)
        Log.d("workManager", "Do work la!")
        val currentTime = System.currentTimeMillis()
        MyPreferenceManager.setString(context, "last_maintenance", getDateTime(currentTime))
        return Result.success()
    }

    private suspend fun uploadResponsesData(context: Context) {
        val uid = MyPreferenceManager.getString(context,context.getString(R.string.user_ID))
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val startOfToday = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val startOfYesterday = startOfToday.minusDays(1)
        val startOfYesterdayString = startOfYesterday.format(formatter)
        val startOfTodayString = startOfToday.format(formatter)
        val yesterdayDate = LocalDate.now().minusDays(1).toString()
        if (validateUid(uid)) {
            val responseDao =
                Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .responseDao()
            val myRef = Firebase.database.getReference("ESM")
            responseDao.getAllYesterday(startOfYesterdayString, startOfTodayString).forEach{entry ->
                myRef.child(uid)
                    .child(yesterdayDate)
                    .child(entry.id.toString())
                    .setValue(ResponseNoId.fromResponse(entry))
            }
        }
    }

    private fun dayCountPlusOne(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val dayCount = sharedPreferences.getInt("count_day", 0)
        editor.putInt("count_day", dayCount + 1)
            .apply()
        Log.d("workManager", "day ${dayCount + 1}")
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

