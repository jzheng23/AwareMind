package com.jianzheng.studyzero.tool

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val context: Context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            uploadData(context)
            dayCountPlusOne(context)
        }
        resetTriggerList(context)
        startMyForegroundService(context)
        Log.d("workManager", "Do work la!")
        val currentTime = System.currentTimeMillis()
        MyPreferenceManager.setString(context, "last_maintenance", getDateTime(currentTime))
        return Result.success()
    }

    private fun dayCountPlusOne(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val dayCount = sharedPreferences.getInt("count_day", 0)
        editor.putInt("count_day", dayCount + 1)
            .apply()
        Log.d("workManager", "day ${dayCount + 1}")
    }

    private fun uploadData(context: Context) {
        val myUsageManager = MyUsageManager()
        myUsageManager.getUsageEvents(context, System.currentTimeMillis(), false)
    }

    private fun resetTriggerList(context: Context) {
        val responseCounter = ResponseCounter(context)
        responseCounter.resetCounter()
        MyDelayManager.resetTriggerList()
    }
}

