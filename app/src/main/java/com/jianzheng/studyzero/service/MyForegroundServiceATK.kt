package com.jianzheng.studyzero.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPreferenceManager

class MyForegroundServiceATK : Service() {
    private var mActivityTransitionsPendingIntent: PendingIntent? = null
    private var mTransitionsReceiver: BroadcastReceiver? = null
    private val transitionsReceiverAction = "com.jzheng23.atk.TRANSITIONS_RECEIVER_ACTION"
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

    private fun getUserActivity(activityType: Int, transitionType: Int): ActivityTransition {
        return ActivityTransition.Builder().setActivityType(activityType)
            .setActivityTransition(transitionType).build()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        initializeActivityTransition()

        mTransitionsReceiver = TransitionsReceiver().also {
            val filter = IntentFilter(transitionsReceiverAction)
            registerReceiver(it, filter, RECEIVER_NOT_EXPORTED)
        }
    }

    private fun initializeActivityTransition() {
        val intent = Intent(this, TransitionsReceiver::class.java)
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Register for transitions updates
        enableActivityTransitions()
    }

    private fun enableActivityTransitions() {
        val request = ActivityTransitionRequest(activityTransitions)

        // The Intent that will be fired when activities are detected
        val intent = Intent(transitionsReceiverAction)
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent!!)

            task.addOnSuccessListener {
//                startForegroundServiceWithNotification()
            }

            task.addOnFailureListener { e ->
            }
        }

    }

    private fun startForegroundServiceWithNotification() {
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification =
            createNotification() // Implement this method to create a notification
        startForeground(1, notification)

        // Place your activity recognition logic here

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        createNotificationChannel() // Ensure the channel is created before building the notification

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE
        )
        val todayCount = MyPreferenceManager.getInt(this, "today_count")
        val totalCount = MyPreferenceManager.getInt(this, "total_count")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Reports: $todayCount today/$totalCount total. Please don't dismiss this notification.")
            .setSmallIcon(R.drawable.heart_alt)
            .setContentIntent(pendingIntent).build()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister the transitions receiver
        mTransitionsReceiver?.let {
            unregisterReceiver(it)
        }

        // Disable activity transitions
        disableActivityTransitions()
    }

    private fun disableActivityTransitions() {
        val activityRecognitionClient = ActivityRecognition.getClient(this)
        mActivityTransitionsPendingIntent?.let { pendingIntent ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                    .addOnSuccessListener {
                        // Handle success
                    }.addOnFailureListener { _ ->
                        // Handle error
                    }
            }

        }
    }

    companion object {
        const val CHANNEL_ID = "foregroundServiceChannel"
    }

    inner class TransitionsReceiver : BroadcastReceiver() {


        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                result?.transitionEvents?.forEach { event ->
                    // Update notification with the current activity
                    val currentActivity =
                        toActivityString(event.activityType) + " " + toTransitionType(event.transitionType)
//                    val notification = createNotification("Current activity: $currentActivity")
                    MyPreferenceManager.setString(context, "current_activity", currentActivity)
                    if (event.activityType == 0) {
                        when (event.transitionType) {
                            0 -> {
                                MyPreferenceManager.setBoolean(context, "is_driving", true)
                            }

                            1 -> {
                                MyPreferenceManager.setBoolean(context, "is_driving", false)
                            }
                        }
                    }
//                    startForeground(1, notification)
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
}