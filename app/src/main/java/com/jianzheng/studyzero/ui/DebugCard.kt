package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.MyReceiverManager
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter
import com.jianzheng.studyzero.tool.resetEverything
import com.jianzheng.studyzero.tool.startEverything
import com.jianzheng.studyzero.tool.startMyForegroundService
import com.jianzheng.studyzero.tool.stopEverything
import java.util.concurrent.TimeUnit

@Composable
fun DebugCard(
    userId: MutableState<String?>,
    modifier: Modifier,
) {
    var isVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val isOverlayAllowed by usePollState {
        MyPermissionManager.checkOverlayPermission(context)
    }
    val isUsageAllowed by usePollState {
        MyPermissionManager.checkUserStatPermission(context)
    }
    val isA11yAllowed by usePollState {
        MyPermissionManager.checkAccessibilityPermission(context)
    }
    val isInputRecording by usePollState {
        sharedPreferences.getBoolean("input_record", false)
    }
    val isTutorialFinished by usePollState {
        sharedPreferences.getBoolean("tutorial_finished", false)
    }
    val isDataCollecting by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("collecting_data", false))
    }
    val isTestTriggers by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("using_testing_triggers", false))
    }
    val isNotificationShowing by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("notification_showing", false))
    }
    val isUnlockReceiverRegistered by usePollState {
        mutableStateOf(MyReceiverManager.checkRegistered())
    }

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
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isVisible) "Collapse" else "Expand"
                )
            }
            if (isVisible) {
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
                    isEnabled = MyPermissionManager.checkNotificationPermission(context),
                    isChecked = isNotificationShowing.value
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                SwitchCard(
                    title = "ESM",
                    desc = "Schedule ESM after unlock",
                    onCheckedChange = {
                        if (it) {
                            MyDelayManager.init(context)
                        } else {
                            MyReceiverManager.unregisterReceiver(context)
                            Log.d("unlock", "unlock unregistered")
                        }
                    },
                    isEnabled = isOverlayAllowed,
                    isChecked = isUnlockReceiverRegistered.value
                )
                SwitchCard(
                    title = "",
                    desc = "Use testing triggers?",
                    onCheckedChange = {
//                    val sharedPreferences =
//                        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
//                    val editor = sharedPreferences.edit()
//                    editor.putBoolean("using_testing_triggers", it)
//                        .apply()
                        MyPreferenceManager.setBoolean(context, "using_testing_triggers", it)
                        MyDelayManager.init(context)
                        resetTriggerFunction(context)
                        val triggerType = if (it) "testing" else "real"
                        val toast =
                            Toast.makeText(
                                context,
                                "Using $triggerType triggers",
                                Toast.LENGTH_SHORT
                            )
                        Handler(Looper.getMainLooper()).post { toast.show() }
                        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 1500)
                    },
                    isEnabled = isOverlayAllowed,
                    isChecked = isTestTriggers.value
                )
                ButtonCard(
                    title = "",
                    desc = "Test the ESM page",
                    onAuthClick = {
                        testSurvey(context)
                    },
                    buttonText = "Test",
                    buttonEnabled = isOverlayAllowed
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                ButtonCard(
                    title = "New Day",
                    desc = "Simulating a new day starts",
                    onAuthClick = {
                        newDayFunction(context)
                        Handler(Looper.getMainLooper()).post {
                            val toast = Toast.makeText(context, "Good morning!", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    },
                    buttonText = "Simulate"
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                ButtonCard(
                    title = "Usage",
                    desc = "Get usage in last 10 seconds",
                    onAuthClick = {
                        val myUsageManager = MyUsageManager()
                        myUsageManager.getUsageEvents(context, System.currentTimeMillis())
                        Handler(Looper.getMainLooper()).post {
                            val toast =
                                Toast.makeText(context, "Usage uploaded!", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    },
                    buttonText = "Get",
                    buttonEnabled = isUsageAllowed
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                SwitchCard(
                    title = "Input",
                    desc = "Record input interaction",
                    onCheckedChange = {
                        MyPreferenceManager.setBoolean(context, "input_record", it)
                    },
                    isEnabled = isA11yAllowed,
                    isChecked = isInputRecording
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                SwitchCard(
                    title = "Tutorial",
                    desc = "Set tutorial as completed",
                    onCheckedChange = {
                        MyPreferenceManager.setBoolean(context, "tutorial_finished", it)
                    },
                    isChecked = isTutorialFinished
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                SwitchCard(
                    title = "Data collection",
                    desc = "Start or pause data collection",
                    onCheckedChange = {
                        if (it) {
                            startEverything(context)
                        } else {
                            stopEverything(context)
                        }
                    },
                    isChecked = isDataCollecting.value
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                ButtonCard(
                    title = "Reset",
                    desc = "Reset to default",
                    onAuthClick  = {
                        resetEverything(context)
                        Handler(Looper.getMainLooper()).post {
                            val toast = Toast.makeText(context, "Reset to default!", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    },
                    buttonText = "Reset"
                )

            }

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

fun testSurvey(context: Context) {
    val serviceIntent = Intent(context, OverlayService::class.java).apply {
        putExtra("tag", "testMet")
    }
    context.startService(serviceIntent)
}

