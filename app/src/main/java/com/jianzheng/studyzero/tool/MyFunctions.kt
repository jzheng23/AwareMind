package com.jianzheng.studyzero.tool

//import com.jianzheng.studyzero.service.MyForegroundService
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jianzheng.studyzero.R
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

/**
 * @param isUserAction true when the participant deliberately starts collection. Only then is
 * the daily worker rescheduled with UPDATE (it may have been pushed 30 days out by
 * [stopEverything]); automatic calls (app launch, reboot) keep the existing schedule.
 */
fun startEverything(context: Context, isUserAction: Boolean = false) {
    if (isUserAction) {
        scheduleDailyMaintenance(context, policy = ExistingPeriodicWorkPolicy.UPDATE)
    }
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
    scheduleDailyMaintenance(context, 2592000000L, ExistingPeriodicWorkPolicy.UPDATE)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.recording_input), false)
    MyPreferenceManager.setBoolean(context, context.getString(R.string.collecting_data), false)
}

fun resetEverything(context: Context) {
    initializeSharedPreference(context)
}

fun startMyForegroundService(context: Context) {
    // MyForegroundService has type "health", which on API 34+ requires ACTIVITY_RECOGNITION;
    // starting it without that permission throws a SecurityException.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.w("startFgs", "ACTIVITY_RECOGNITION not granted, cannot start foreground service")
        return
    }
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // Can only ask for permissions from an Activity; workers, services and receivers
        // used to crash here with a ClassCastException.
        val activity = context as? Activity
        if (activity != null) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )
        } else {
            Log.w("startFgs", "Location permission missing, cannot start foreground service")
        }
    } else {
        val myForegroundServiceIntent = Intent(context, MyForegroundService::class.java)
        try {
            ContextCompat.startForegroundService(context, myForegroundServiceIntent)
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (API 31+) when started from background.
            Log.e("startFgs", "Foreground service start not allowed", e)
        }
    }
}

fun scheduleDailyMaintenance(
    context: Context,
    initialDelayInput: Long = 0L,
    policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
) {
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
        policy,
        workRequest
    )
//    val workInfo = myWorkManager.getWorkInfoById(workRequest.id)
//    val workState = workInfo.get()?.state
//    Log.d("workManager", "scheduled! $workState id ${workRequest.id}")
}

fun getInitialDelay(): Long {
    val currentDate = Calendar.getInstance()
    val hourInt = 3
    val randomInt = Random.nextInt(0, 60)
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hourInt)
        set(Calendar.MINUTE, randomInt)
        set(Calendar.SECOND, 0)
        // If it's after or equal 3 AM, schedule for the next day
        if (before(currentDate)) {
            add(Calendar.HOUR_OF_DAY, 24)
        }
    }
//    Log.d("workManager", "Set at $hourInt:$randomInt")
    return dueDate.timeInMillis - currentDate.timeInMillis
}

fun initializeSharedPreference(context: Context) {
    val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putInt(context.getString(R.string.count_today), 0)
            .putInt(context.getString(R.string.count_total), 0)
            .putInt(context.getString(R.string.count_day), 0)
            .putBoolean(context.getString(R.string.using_once_triggers), false)
            .putBoolean(context.getString(R.string.using_testing_triggers), false)
            .putBoolean(context.getString(R.string.is_first_run), false)
            .putString(context.getString(R.string.user_ID), "")
//        .putBoolean(context.getString(R.string.is_UID_valid), false)
            .putInt(context.getString(R.string.count_20_met), 0)
            .putInt(context.getString(R.string.count_30_met), 0)
            .putInt(context.getString(R.string.count_once_met), 0)
            .putInt(context.getString(R.string.count_random_met), 0)
            .putBoolean(context.getString(R.string.recording_input), false)
            .putBoolean(context.getString(R.string.tutorial_finished), false)
            .putBoolean(context.getString(R.string.collecting_data), false)
            .putBoolean(context.getString(R.string.is_tester), false)
            .putBoolean(context.getString(R.string.showing_notification), false)
    }
}

