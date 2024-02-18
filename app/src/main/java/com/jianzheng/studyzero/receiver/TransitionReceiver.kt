package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import com.jianzheng.studyzero.tool.MyPreferenceManager

class TransitionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                // Update notification with the current activity
                if (event.activityType == 0) {
                    when(event.transitionType) {
                        0 -> {
                            MyPreferenceManager.setBoolean(context,"is_driving", true)
                        }
                        1 -> {
                            MyPreferenceManager.setBoolean(context,"is_driving", false)
                        }
                    }
                }
            }
        }
    }


}