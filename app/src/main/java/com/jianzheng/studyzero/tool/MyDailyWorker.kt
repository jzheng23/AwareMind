package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jianzheng.studyzero.service.DailyMaintenance

class MyDailyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val context: Context = applicationContext
        uploadData(context)
        resetTriggerList(context)
        dayCountPlusOne(context)
        return Result.success()
    }

    private fun dayCountPlusOne(context: Context) {
        val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val dayCount = sharedPreferences.getInt("day_count", 0)
        editor.putInt("day_count", dayCount + 1)
            .apply()
    }

    private fun uploadData(context: Context) {
        MyUsageManager.getUsageEvents(context)
    }

    private fun resetTriggerList(context: Context) {
        val responseCounter = ResponseCounter(context)
        responseCounter.resetCounter()
        MyDelayManager.resetTriggerList()
    }
}

