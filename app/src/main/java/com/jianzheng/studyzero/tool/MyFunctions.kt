package com.jianzheng.studyzero.tool

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jianzheng.studyzero.R
//import com.jianzheng.studyzero.service.MyForegroundService
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
//    scheduleDailyMaintenance(context)
//    MyDelayManager.init(context)
//    MyReceiverManager.registerReceiver(context)
    MyPreferenceManager.setBoolean(
        context,
        context.getString(R.string.using_testing_triggers), false
    )
    MyPreferenceManager.setBoolean(context, context.getString(R.string.using_once_triggers), false)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.recording_input), true)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.collecting_data), true)
}

fun stopEverything(context: Context) {
    context.stopService(Intent(context, MyForegroundService::class.java))
//    MyReceiverManager.unregisterReceiver(context)
    scheduleDailyMaintenance(context, 2592000000L)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.recording_input), false)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.collecting_data), false)
}

fun resetEverything(context: Context) {
    initializeSharedPreference(context)
}

fun startMyForegroundService(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            0
        )
    } else {
        val myForegroundServiceIntent = Intent(context, MyForegroundService::class.java)
        ContextCompat.startForegroundService(context, myForegroundServiceIntent)
    }
}

fun scheduleDailyMaintenance(context: Context, initialDelayInput: Long = 0L) {
    val myWorkManager = WorkManager.getInstance(context)

    val initialDelay = if (initialDelayInput == 0L) {
        getInitialDelay()
    } else {
        initialDelayInput
    }
    val workRequest = PeriodicWorkRequestBuilder<MyDailyWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()
    myWorkManager.enqueueUniquePeriodicWork(
        "daily_maintenance",
        ExistingPeriodicWorkPolicy.UPDATE, // KEEP or UPDATE
        workRequest
    )
    val workInfo = myWorkManager.getWorkInfoById(workRequest.id)
    val workState = workInfo.get()?.state
    Log.d("workManager", "scheduled! $workState id ${workRequest.id}")
}

fun getInitialDelay(): Long {
    val currentDate = Calendar.getInstance()
    val hourInt = 3
    val randomInt = Random.nextInt(0, 60)
    val dueDate = Calendar.getInstance().apply {
        // Set time to 11 AM for testing, 4 AM for real
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

fun initializeSharedPreference(context: Context) {
    val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putInt(context.getString(R.string.count_today), 0)
        .putInt(context.getString(R.string.count_total), 0)
        .putInt(context.getString(R.string.count_day), 0)
        .putBoolean(context.getString(R.string.using_once_triggers), false)
        .putBoolean(context.getString(R.string.using_testing_triggers), false)
        .putBoolean(context.getString(R.string.is_first_run), false)
        .putString(context.getString(R.string.user_ID), "")
        .putBoolean(context.getString(R.string.is_UID_valid), false)
        .putInt(context.getString(R.string.count_20_met), 0)
        .putInt(context.getString(R.string.count_30_met), 0)
        .putInt(context.getString(R.string.count_once_met), 0)
        .putInt(context.getString(R.string.count_random_met), 0)
        .putBoolean(context.getString(R.string.recording_input), false)
        .putBoolean(context.getString(R.string.tutorial_finished), false)
        .putBoolean(context.getString(R.string.collecting_data), false)
        .putBoolean(context.getString(R.string.is_tester), false)
        .putBoolean(context.getString(R.string.showing_notification), false)
        .apply()
}

