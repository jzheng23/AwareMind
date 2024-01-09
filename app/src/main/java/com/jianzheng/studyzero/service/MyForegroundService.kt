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
import androidx.room.Room
import com.jianzheng.studyzero.MainActivity
import com.jianzheng.studyzero.data.EsmDatabase
import com.jianzheng.studyzero.data.ResponseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class MyForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var countOfResponses: Flow<Int>
    private lateinit var responseDao: ResponseDao


    override fun onCreate() {
        super.onCreate()
//        Log.d("fore", "onCreate")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val db =
            Room.databaseBuilder(applicationContext, EsmDatabase::class.java, "response_database")
                .fallbackToDestructiveMigration()
                .build()
        responseDao = db.responseDao()
        createNotificationChannel()
        updateCount()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        createNotificationChannel()
        updateCount()
        return START_STICKY
    }

    private fun updateCount() {
        countOfResponses = responseDao.getCountToday(todayStarts())
        serviceScope.launch {
            countOfResponses.collect {
                val notification = buildForegroundNotification(it)
                startForeground(notificationId, notification)
                Log.d("notification", "count updated!")
            }
        }
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

    private fun buildForegroundNotification(count: Int = 0): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Study Zero")
            .setContentText("$count prompts completed today")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Set an icon for the notification
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val notificationId = 1
        const val channelId = "timer_channel"
    }

    private fun todayStarts(): Long {
        val startOfDayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return startOfDayCalendar.timeInMillis
    }
}
