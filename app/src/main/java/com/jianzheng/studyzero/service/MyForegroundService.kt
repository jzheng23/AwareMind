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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.activity.ActivityDatabase
import com.jianzheng.studyzero.data.activity.PhysicalActivity
import com.jianzheng.studyzero.data.activity.toFirebaseActivity
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.UnlockReceiverManager
import com.jianzheng.studyzero.tool.scheduleDailyMaintenance
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class MyForegroundService : Service() {
    private lateinit var uid: String
    private val myRefActivity = Firebase.database.getReference("Activity")
    private var mActivityTransitionsPendingIntent: PendingIntent? = null
    private var mTransitionsReceiver: BroadcastReceiver? = null
    private val transitionsReceiverAction = "com.jzheng23.studyzero.TRANSITIONS_RECEIVER_ACTION"
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

    private fun testPendingIntent() {
        Log.d("ATR", "Testing broadcast receiver manually")
        val testIntent = Intent(transitionsReceiverAction)
        testIntent.setPackage(packageName)
        testIntent.putExtra("test", "manual_trigger")
        sendBroadcast(testIntent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        Log.d("ATR", "MyForegroundService onCreate() called")
        uid = MyPreferenceManager.getString(this, this.getString(R.string.user_ID))
//        activityDao = Room.databaseBuilder(this, ActivityDatabase::class.java, "activity_database")
        initializeActivityTransition()
        mTransitionsReceiver = TransitionsReceiver().also {
            val filter = IntentFilter(transitionsReceiverAction)
            registerReceiver(it, filter, RECEIVER_NOT_EXPORTED)
        }
        scheduleDailyMaintenance(this)
        MyDelayManager.init(this)
        UnlockReceiverManager.registerReceiver(this)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun initializeActivityTransition() {
//        Log.d("ATR", "activity recognition initialized")
        val request = ActivityTransitionRequest(activityTransitions)
        // The Intent that will be fired when activities are detected
        val intent = Intent(transitionsReceiverAction)
        intent.setPackage(packageName)
//        val explicitIntent = Intent(this, TransitionsReceiver::class.java)
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(
            this,
            TRANSITION_INTENT_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent!!)
            Log.d("ATR", "task created")

            task.addOnSuccessListener {
//                startForegroundServiceWithNotification()
                Log.d("ATR", "success lister")
            }

            task.addOnFailureListener { _ ->
                Log.d("ATR", "failure listener")
            }
        } else {
            Log.d("ATR", "activity recognition permission not allowed")
        }

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ATR", "MyForegroundService onStartCommand() called")
        val notification =
            createNotification() // Implement this method to create a notification
        startForeground(1, notification)
        MyPreferenceManager.setBoolean(this, this.getString(R.string.showing_notification), true)

        testPendingIntent()

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
            this, NOTIFICATION_INTENT_CODE, notificationIntent, PendingIntent.FLAG_MUTABLE
        )
        val todayCount = MyPreferenceManager.getInt(this, this.getString(R.string.count_today))
        val totalCount = MyPreferenceManager.getInt(this, this.getString(R.string.count_total))
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Reports: $todayCount today/$totalCount total. Please don't dismiss this notification.")
            .setSmallIcon(R.drawable.heart_alt)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent).build()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister the transitions receiver
        MyPreferenceManager.setBoolean(this, this.getString(R.string.showing_notification), false)
        mTransitionsReceiver?.let {
            unregisterReceiver(it)
        }
        UnlockReceiverManager.unregisterReceiver(this)
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
        const val CHANNEL_ID = "AwareMindServiceChannel"
        const val NOTIFICATION_INTENT_CODE = 1
        const val TRANSITION_INTENT_CODE = 2
    }

    inner class TransitionsReceiver : BroadcastReceiver() {


        override fun onReceive(context: Context, intent: Intent) {
            Log.d("ATR", "on receive")

            if (intent.hasExtra("test")) {
                Log.d("ATR", "Test broadcast received: ${intent.getStringExtra("test")}")
                return
            }

            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                result?.transitionEvents?.forEach { event ->
                    // Update notification with the current activity
                    val currentActivity =
                        toActivityString(event.activityType)
                    Log.d("ATR", "current activity: $currentActivity")
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        MyPreferenceManager.setString(context, "current_activity", currentActivity)
                        logActivity(context, event.activityType)
                    }
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
                }
            } else {
                Log.d("ATR", "No ActivityTransitionResult in intent")
            }
        }

        private fun toActivityString(activity: Int) = when (activity) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            else -> "UNKNOWN"
        }
    }

    private fun logActivity(context: Context, activityType: Int) {
        val currentTime = System.currentTimeMillis()
        val todayDate = LocalDate.now().toString()
        val activityRecord = PhysicalActivity(startTime = currentTime, activityType = activityType)
        val activityDao =
            Room.databaseBuilder(context, ActivityDatabase::class.java, "activity_database")
                .fallbackToDestructiveMigration(false)
                .build()
                .activityDao()

        CoroutineScope(Dispatchers.IO).launch {
            val generatedId = activityDao.insert(activityRecord)
            if (validateUid(uid)) {
                myRefActivity
                    .child(uid)
                    .child(todayDate)
                    .child(generatedId.toString())
                    .setValue(activityRecord.toFirebaseActivity())
            }
        }

    }
}