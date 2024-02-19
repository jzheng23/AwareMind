package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
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
                val currentActivity =
                    toActivityString(event.activityType) + " " + toTransitionType(event.transitionType)
                MyPreferenceManager.setString(context, "current_activity", currentActivity)
            }
        }
    }

    private fun toActivityString(activity: Int) = when (activity) {
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.RUNNING -> "RUNNING"
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        else -> "UNKNOWN"
    }

    private fun toTransitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }


}