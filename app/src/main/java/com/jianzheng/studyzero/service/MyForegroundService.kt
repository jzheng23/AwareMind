package com.jianzheng.studyzero.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPreferenceManager

class MyForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager


    override fun onCreate() {
        super.onCreate()
//        Log.d("fore", "onCreate")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        updateCount()
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
            .setContentTitle("Study Zero")
            .setContentText("$countFromPrefs prompts completed today")
//            .setSmallIcon(android.R.drawable.ic_dialog_info) // Set an icon for the notification
            .setSmallIcon(R.drawable.heart_full)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val notificationId = 1
        const val channelId = "timer_channel"
    }

}
