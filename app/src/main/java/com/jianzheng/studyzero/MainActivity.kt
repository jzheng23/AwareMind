package com.jianzheng.studyzero

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
    private lateinit var auth: FirebaseAuth


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

// ...
// Initialize Firebase Auth
        auth = Firebase.auth
    }

    private fun scheduleDailyMaintenance() {
        val initialDelay = 5_000L
        val myWorkManager = WorkManager.getInstance(this)

        val workRequest = PeriodicWorkRequestBuilder<MyDailyWorker>(24, TimeUnit.HOURS)
            // Additional configuration (like constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        myWorkManager.enqueueUniquePeriodicWork(
            "daily_maintenance",
            ExistingPeriodicWorkPolicy.UPDATE, // KEEP or UPDATE
            workRequest
//            workRequestTest
        )
        val workInfo = myWorkManager.getWorkInfoById(workRequest.id)
        val workState = workInfo.get()?.state
        Log.d("workManager", "scheduled! $workState id ${workRequest.id}")

//        val workRequestTest = OneTimeWorkRequestBuilder<MyDailyWorker>()
//            .setInitialDelay(initialDelay * 5, TimeUnit.MILLISECONDS)
//            .build()
//        myWorkManager.enqueue(workRequestTest)
//        val workInfoTest = myWorkManager.getWorkInfoById(workRequestTest.id)
//        val workStateTest = workInfoTest.get()?.state
//        Log.d("workManager", "scheduled test! $workStateTest id ${workRequestTest.id}")
    }

    private fun getInitialDelay(): Long {
        val currentDate = Calendar.getInstance()
        val hourInt = 11
        val randomInt = Random.nextInt(0, 60)
        val dueDate = Calendar.getInstance().apply {
            // Set time to 10 AM for testing, 10 AM for real
            set(Calendar.HOUR_OF_DAY, hourInt)
            set(Calendar.MINUTE, randomInt)
            set(Calendar.SECOND, 0)
            // If it's after or equal 4 AM, schedule for the next day
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }
        Log.d("workManager", "Set at $hourInt:$randomInt")
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

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInAnonymously:success")
                    val user = auth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Authentication", "signInAnonymously:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

}