package com.jianzheng.studyzero.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.UnlockReceiverManager
import com.jianzheng.studyzero.tool.scheduleDailyMaintenance

class MyForegroundService : Service() {
    private lateinit var uid: String

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        uid = MyPreferenceManager.getString(this, this.getString(R.string.user_ID))

        scheduleDailyMaintenance(this)
        MyDelayManager.init(this)
        UnlockReceiverManager.registerReceiver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Call startForeground() IMMEDIATELY - before any other work
        val notification = createNotification()
        startForeground(1, notification)
        MyPreferenceManager.setBoolean(this, this.getString(R.string.showing_notification), true)

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
        MyPreferenceManager.setBoolean(this, this.getString(R.string.showing_notification), false)
        UnlockReceiverManager.unregisterReceiver(this)
    }

    companion object {
        const val CHANNEL_ID = "AwareMindServiceChannel"
        const val NOTIFICATION_INTENT_CODE = 1
    }
}
