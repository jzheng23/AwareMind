package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jianzheng.studyzero.service.OverlayService

class MyTriggerWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val tag = inputData.getString("tag")
        val currentTimeMillis = inputData.getLong("unlock", System.currentTimeMillis())
        val serviceIntent = Intent(applicationContext, OverlayService::class.java).apply {
            putExtra("tag", tag)
            putExtra("unlock", currentTimeMillis)
            Log.d("delayTime", "passed in myWorker: $currentTimeMillis")
        }
        Log.d("unlock", "triggered by $tag")
        applicationContext.startService(serviceIntent)
        return Result.success()
    }
}