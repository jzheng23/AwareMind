package com.jianzheng.studyzero.tool

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val context: Context = applicationContext
        uploadData(context)
        resetTriggerList(context)
        dayCountPlusOne(context)
        Log.d("dayCount","Do work la!")
        return Result.success()
    }

    private fun dayCountPlusOne(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val dayCount = sharedPreferences.getInt("day_count", 0)
        editor.putInt("day_count", dayCount + 1)
            .apply()
        Log.d("dayCount","day ${dayCount+1}")
    }

    private fun uploadData(context: Context) {
        val myUsageManager = MyUsageManager()
        myUsageManager.getUsageEvents(context, System.currentTimeMillis())
    }

    private fun resetTriggerList(context: Context) {
        val responseCounter = ResponseCounter(context)
        responseCounter.resetCounter()
        MyDelayManager.resetTriggerList()
    }
}

