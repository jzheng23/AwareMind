package com.jianzheng.studyzero.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.service.MyForegroundService
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyDailyWorker
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter
import com.jianzheng.studyzero.tool.startMyForegroundService
import java.util.concurrent.TimeUnit

@Composable
fun DebugCard(
    userId: MutableState<String?>,
    modifier: Modifier,
    showDialog: MutableState<Boolean>
) {
    val context = LocalContext.current
    Card(
//        modifier = Modifier.padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(mediumPadding)
        ) {
            Text("User ID is ${userId.value}, upload is ${validateUid(userId.value)}")
            SwitchCard(
                title = "Notification",
                desc = "Show a persistent notification",
                onCheckedChange = {
                    if (it) {
                        startMyForegroundService(context)
                    } else {
                        context.stopService(Intent(context, MyForegroundService::class.java))
                    }
                },
                isEnabled = MyPermissionManager.checkNotificationPermission(context)
            )
            Divider()
            SwitchCard(
                title = "ESM",
                desc = "Schedule ESM after unlock",
                onCheckedChange = {
                    if (it) {
                        MyDelayManager.init(context)
                    } else {
                        MyDelayManager.unregisterUnlock(context)
                    }
                },
                isEnabled = MyPermissionManager.checkOverlayPermission(context)
            )
            SwitchCard(
                title = "",
                desc = "Use testing triggers?",
                onCheckedChange = {
                    val sharedPreferences =
                        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("using_testing_triggers", it)
                        .apply()
                    MyDelayManager.init(context)
                    resetTriggerFunction(context)
                    val triggerType = if (it) "testing" else "real"
                    val toast =
                        Toast.makeText(context, "Using $triggerType triggers", Toast.LENGTH_SHORT)
                    Handler(Looper.getMainLooper()).post { toast.show() }
                    Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 1500)
                },
                isEnabled = true
            )
            AuthCard(
                title = "",
                desc = "Test the ESM page",
                onAuthClick = {
                    testFunction(context)
                },
                buttonText = "Test"
            )
            Divider()
            AuthCard(
                title = "New Day",
                desc = "Simulating a new day starts",
                onAuthClick = {
                    newDayFunction(context)
                    Handler(Looper.getMainLooper()).post {
                        val toast = Toast.makeText(context, "Good morning!", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                },
                buttonText = "Reset"
            )
            Divider()
            AuthCard(
                title = "Usage",
                desc = "Get usage in last 10 seconds",
                onAuthClick = {
                    val myUsageManager = MyUsageManager()
                    myUsageManager.getUsageEvents(context, System.currentTimeMillis())
                    Handler(Looper.getMainLooper()).post {
                        val toast = Toast.makeText(context, "Usage uploaded!", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                },
                buttonText = "Get",
                buttonEnabled = MyPermissionManager.checkUserStatPermission(context)
            )
        }
    }
}

fun newDayFunction(context: Context) {
//    val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
//    val editor = sharedPreferences.edit()
//    val dayCount = sharedPreferences.getInt("day_count", 0)
//    editor.putInt("day_count", dayCount + 1)
//        .apply()
//    MyUsageManager.getUsageEvents(context)
//    resetTriggerFunction(context)
    val workRequestTest = PeriodicWorkRequestBuilder<MyDailyWorker>(15, TimeUnit.MINUTES)
        .setInitialDelay(1000, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_maintenance_test",
        ExistingPeriodicWorkPolicy.UPDATE, // KEEP or REPLACE
        workRequestTest
    )
//    val workRequestOneTime = OneTimeWorkRequestBuilder<MyDailyWorker>()
//        .setInitialDelay(1000, TimeUnit.MILLISECONDS)
//        .build()
//    WorkManager.getInstance(context).enqueue(workRequestOneTime)
    Log.d("dayCount", "new day reset")
}

fun resetTriggerFunction(context: Context) {
    val responseCounter = ResponseCounter(context)
    responseCounter.resetCounter()
    MyDelayManager.resetTriggerList()
}

fun testFunction(context: Context) {
    val serviceIntent = Intent(context, OverlayService::class.java).apply {
        putExtra("tag", "testMet")
    }
    context.startService(serviceIntent)
}

fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}