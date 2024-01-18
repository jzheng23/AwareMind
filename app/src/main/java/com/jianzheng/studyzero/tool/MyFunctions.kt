package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jianzheng.studyzero.service.MyAccessibilityService
import com.jianzheng.studyzero.service.MyForegroundService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun getDateTime(timeInMillis: Long): String {
    val date = Date(timeInMillis)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    return formatter.format(date)
}

fun startEverything(context: Context) {
    startMyForegroundService(context)
    createSharedPreference(context)
    MyDelayManager.init(context)
    scheduleDailyMaintenance(context)
    startMyAccessibilityService(context)
}

fun stopEverything(context: Context){
    context.stopService(Intent(context, MyForegroundService::class.java))
    MyDelayManager.unregisterUnlock(context)
    MyPreferenceManager.setBoolean(context, "input_record", false)
    scheduleDailyMaintenance(context, 2592000000L)
}


fun startMyAccessibilityService(context: Context) {
    MyPreferenceManager.setBoolean(context, "input_record", true)
}

fun startMyForegroundService(context: Context) {
    val myForegroundServiceIntent = Intent(context, MyForegroundService::class.java)
    ContextCompat.startForegroundService(context, myForegroundServiceIntent)
}

fun scheduleDailyMaintenance(context: Context, initialDelay: Long = 5000L) {
    val myWorkManager = WorkManager.getInstance(context)

    val workRequest = PeriodicWorkRequestBuilder<MyDailyWorker>(24, TimeUnit.HOURS)
        // Additional configuration (like constraints)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()
    myWorkManager.enqueueUniquePeriodicWork(
        "daily_maintenance",
        ExistingPeriodicWorkPolicy.UPDATE, // KEEP or UPDATE
        workRequest
//            workRequestTest
    )
    val workInfo = myWorkManager.getWorkInfoById(workRequest.id)
    val workState = workInfo.get()?.state
    Log.d("workManager", "scheduled! $workState id ${workRequest.id}")

//        val workRequestTest = OneTimeWorkRequestBuilder<MyDailyWorker>()
//            .setInitialDelay(initialDelay * 5, TimeUnit.MILLISECONDS)
//            .build()
//        myWorkManager.enqueue(workRequestTest)
//        val workInfoTest = myWorkManager.getWorkInfoById(workRequestTest.id)
//        val workStateTest = workInfoTest.get()?.state
//        Log.d("workManager", "scheduled test! $workStateTest id ${workRequestTest.id}")
}

fun getInitialDelay(): Long {
    val currentDate = Calendar.getInstance()
    val hourInt = 11
    val randomInt = Random.nextInt(0, 60)
    val dueDate = Calendar.getInstance().apply {
        // Set time to 10 AM for testing, 10 AM for real
        set(Calendar.HOUR_OF_DAY, hourInt)
        set(Calendar.MINUTE, randomInt)
        set(Calendar.SECOND, 0)
        // If it's after or equal 4 AM, schedule for the next day
        if (before(currentDate)) {
            add(Calendar.HOUR_OF_DAY, 24)
        }
    }
    Log.d("workManager", "Set at $hourInt:$randomInt")
    return dueDate.timeInMillis - currentDate.timeInMillis
}

fun createSharedPreference(context: Context) {
    val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    if (sharedPreferences.getBoolean("first_run", true)) {
        editor.putBoolean("first_run", false)
            .putString("UID", "null")
            .apply()
    }
}

