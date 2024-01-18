package com.jianzheng.studyzero.receiver

import android.content.Context
import android.content.Intent
import android.util.Log
import android.content.BroadcastReceiver
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityDetectionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("drivingCheck","on receive")
        if (ActivityRecognitionResult.hasResult(intent)) {
            Log.d("drivingCheck","has intent")
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity
            val isDriving = mostProbableActivity?.type == DetectedActivity.IN_VEHICLE

        }
    }
}