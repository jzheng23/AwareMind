package com.jianzheng.studyzero

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jianzheng.studyzero.receiver.UnlockReceiver
import com.jianzheng.studyzero.service.MyForegroundService
import com.jianzheng.studyzero.tool.MyDailyWorker
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.ui.SettingPage
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val unlockReceiver = UnlockReceiver()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudyZeroTheme {
                Surface {
                    SettingPage()
                }
            }
        }

        //register receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(unlockReceiver, filter)
        //start foreground service
        val myForegroundServiceIntent = Intent(this, MyForegroundService::class.java)
        ContextCompat.startForegroundService(this, myForegroundServiceIntent)

        //start room TODO maybe not needed?
//        val db = Room.databaseBuilder(applicationContext, EsmDatabase::class.java,"response_database").build()
//        val responseDao = db.responseDao()
//        GlobalScope.launch(Dispatchers.IO) {
//            responseDao.insert(Response(id = 1, delay = 0, answer1 = 9, answer2 = 9, startingTime = 0, submittingTime = 0))
//        }

        //init sharedPreference, TODO maybe not needed?
        val sharedPreferences =
            this.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("first_run", true)) createSharedPreference()

        MyDelayManager.init(this)
        scheduleDailyMaintenance()
    }

    private fun scheduleDailyMaintenance() {
        val workRequest = PeriodicWorkRequestBuilder<MyDailyWorker>(24, TimeUnit.HOURS)
            // Additional configuration (like constraints)
            .setInitialDelay(getInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_maintenance",
            ExistingPeriodicWorkPolicy.KEEP, // KEEP or REPLACE
            workRequest
        )
    }

    private fun getInitialDelay(): Long {
        val currentDate = Calendar.getInstance()
        val randomInt = Random.nextInt(0, 60)
        val dueDate = Calendar.getInstance().apply {
            // Set time to 4 AM
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, randomInt)
            set(Calendar.SECOND, 0)
            // If it's after or equal 4 AM, schedule for the next day
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }
        return dueDate.timeInMillis - currentDate.timeInMillis
    }

    private fun createSharedPreference() {
        val sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

        // Open the editor to make changes
        val editor = sharedPreferences.edit()

        // Put values
        editor.putBoolean("first_run", false)
            .putString("UID", "null")
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
    }


}