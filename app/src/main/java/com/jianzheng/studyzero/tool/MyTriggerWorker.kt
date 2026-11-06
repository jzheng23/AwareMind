package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jianzheng.studyzero.service.OverlayService

class MyTriggerWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val tag = inputData.getString("tag")
        val currentTimeMillis = inputData.getLong("unlock", System.currentTimeMillis())
        val isListTrigger = inputData.getBoolean(MyDelayManager.KEY_IS_LIST_TRIGGER, false)

        if (isListTrigger) {
            MyDelayManager.onRandomTriggerFired(applicationContext, id.toString())
        }

        // Fire-time check: never show a survey on a screen that is off.
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            Log.d("unlock", "trigger $tag skipped: screen off")
            if (isListTrigger) MyDelayManager.recycleTrigger(applicationContext)
            return Result.success()
        }

        // Skip random and fixed surveys while a navigation app is in use.
        val skippable = tag == "count_random_met" || tag == "count_20_met" || tag == "count_30_met"
        if (skippable && MyUsageManager.isNavigationAppCurrentlyInUse(applicationContext)) {
            Log.d("unlock", "trigger $tag skipped: navigating")
            if (isListTrigger) MyDelayManager.recycleTrigger(applicationContext)
            return Result.success()
        }

        val serviceIntent = Intent(applicationContext, OverlayService::class.java).apply {
            putExtra("tag", tag)
            putExtra("unlock", currentTimeMillis)
        }
        try {
            applicationContext.startService(serviceIntent)
        } catch (e: IllegalStateException) {
            Log.e("unlock", "Could not start OverlayService", e)
            if (isListTrigger) MyDelayManager.recycleTrigger(applicationContext)
        }
        return Result.success()
    }
}
