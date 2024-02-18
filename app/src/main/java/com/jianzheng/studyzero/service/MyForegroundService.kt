package com.jianzheng.studyzero.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.receiver.TransitionsReceiver
import com.jianzheng.studyzero.tool.MyPreferenceManager

class MyForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var mActivityTransitionsPendingIntent: PendingIntent? = null
    private var mTransitionsReceiver: BroadcastReceiver? = null
    private val transitionsReceiverAction = "com.jianzheng23.studyzero.TRANSITIONS_RECEIVER_ACTION"
    private val activityTransitions: List<ActivityTransition> by lazy {
        listOf(
            getUserActivity(
                DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ),
            getUserActivity(
                DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ),
            getUserActivity(
                DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ),
            getUserActivity(
                DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ),
            getUserActivity(
                DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ),
            getUserActivity(
                DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ),
            getUserActivity(
                DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ),
            getUserActivity(
                DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ),
            getUserActivity(
                DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER
            ),
            getUserActivity(
                DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
//        Log.d("fore", "onCreate")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        updateCount()
        initializeActivityTransition()

        mTransitionsReceiver = TransitionsReceiver().also {
            val filter = IntentFilter(transitionsReceiverAction)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(it, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(it, filter)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        createNotificationChannel()
        updateCount()
        MyPreferenceManager.setBoolean(this, "notification_showing", true)
        return START_STICKY
    }

    private fun updateCount() {
        val notification = buildForegroundNotification()
        startForeground(notificationId, notification)
        Log.d("notification", "count updated!")
    }

    // Inside MyForegroundService class
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }


    private fun buildForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val countFromPrefs = MyPreferenceManager.getInt(this, "today_count")
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$countFromPrefs prompts completed today")
//            .setSmallIcon(android.R.drawable.ic_dialog_info) // Set an icon for the notification
            .setSmallIcon(R.drawable.heart_alt)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun getUserActivity(activityType: Int, transitionType: Int): ActivityTransition {
        return ActivityTransition.Builder().setActivityType(activityType)
            .setActivityTransition(transitionType).build()
    }
    private fun initializeActivityTransition() {
        val intent = Intent(this, TransitionsReceiver::class.java)
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val request = ActivityTransitionRequest(activityTransitions)

        // Register for transitions updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent!!)

            task.addOnSuccessListener {
                // Handle success
            }

            task.addOnFailureListener { e ->
                // Handle failure
            }
        }
    }

    companion object {
        const val notificationId = 1
        const val channelId = "timer_channel"
    }

}
