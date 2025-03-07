package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Schedule the work instead of starting service directly
            val workRequest = OneTimeWorkRequestBuilder<RebootWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

class RebootWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Add the logic that was in your RebootService
        return Result.success()
    }
}