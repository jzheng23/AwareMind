package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter
import com.jianzheng.studyzero.tool.resetEverything
import com.jianzheng.studyzero.tool.startEverything
import com.jianzheng.studyzero.tool.startMyForegroundService
import com.jianzheng.studyzero.tool.stopEverything
import java.util.concurrent.TimeUnit

@Composable
fun DebugCard(
//    navController: NavController,
    userId: String = "",
) {
    var isVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val isOverlayAllowed by remember {
        mutableStateOf(MyPermissionManager.checkOverlayPermission(context))
    }
    val isUsageAllowed by remember {
        mutableStateOf(MyPermissionManager.checkUserStatPermission(context))
    }
//    val isA11yAllowed by remember {
//        mutableStateOf(MyPermissionManager.checkAccessibilityPermission(context))
//    }
//    val isInputRecording by usePollState {
//        sharedPreferences.getBoolean("input_record", false)
//    }
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
        mutableStateOf(sharedPreferences.getBoolean("showing_notification", false))
    }
//    val isUnlockReceiverRegistered by usePollState {
//        mutableStateOf(sharedPreferences.getBoolean("unlock_registered", false))
//    }
//    val isDriving by usePollState {
//        mutableStateOf(sharedPreferences.getBoolean("is_driving", false))
//    }
    val currentActivity by usePollState {
        mutableStateOf(sharedPreferences.getString("current_activity", "unknown"))
    }

    Card(
//        modifier = Modifier.padding(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = mediumPadding, vertical = mediumPadding / 3)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Debug",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { isVisible = !isVisible },
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isVisible) "Collapse" else "Expand"
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            if (isVisible) {
//                Text("User ID is $userId, upload is ${validateUid(userId)}")
//                HorizontalDivider(
//                    color = MaterialTheme.colorScheme.outline
//                )
                Text("Current activity: ${currentActivity.value}")
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
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
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
//                SwitchCard(
//                    title = "ESM",
//                    desc = "Schedule ESM after unlock",
//                    onCheckedChange = {
//                        if (it) {
//                            MyDelayManager.init(context)
//                        } else {
//                            MyReceiverManager.unregisterReceiver(context)
//                            Log.d("unlock", "unlock unregistered")
//                        }
//                    },
//                    isEnabled = isOverlayAllowed,
//                    isChecked = isUnlockReceiverRegistered.value
//                )
                SwitchCard(
                    title = "",
                    desc = "Use testing triggers?",
                    onCheckedChange = {
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
//                HorizontalDivider(
//                    color = MaterialTheme.colorScheme.outline
//                )
//                ButtonCard(
//                    title = "New Day",
//                    desc = "Simulating a new day starts",
//                    onAuthClick = {
//                        newDayFunction(context)
//                        Handler(Looper.getMainLooper()).post {
//                            val toast = Toast.makeText(context, "Good morning!", Toast.LENGTH_SHORT)
//                            toast.show()
//                        }
//                    },
//                    buttonText = "Simulate"
//                )
//                HorizontalDivider(
//                    color = MaterialTheme.colorScheme.outline
//                )
//                ButtonCard(
//                    title = "Usage",
//                    desc = "Get usage in last 10 seconds",
//                    onAuthClick = {
//                        val myUsageManager = MyUsageManager()
//                        myUsageManager.getUsageEvents(context, System.currentTimeMillis())
//                        Handler(Looper.getMainLooper()).post {
//                            val toast =
//                                Toast.makeText(context, "Usage uploaded!", Toast.LENGTH_SHORT)
//                            toast.show()
//                        }
//                    },
//                    buttonText = "Get",
//                    buttonEnabled = isUsageAllowed
//                )
//                Divider(
//                    color = MaterialTheme.colorScheme.outline
//                )
//                SwitchCard(
//                    title = "Input",
//                    desc = "Record input interaction",
//                    onCheckedChange = {
//                        MyPreferenceManager.setBoolean(context, "input_record", it)
//                    },
//                    isEnabled = isA11yAllowed,
//                    isChecked = isInputRecording
//                )

//                HorizontalDivider(
//                    color = MaterialTheme.colorScheme.outline
//                )
//                SwitchCard(
//                    title = "Driving",
//                    desc = "I am driving",
//                    onCheckedChange = {
//                        MyPreferenceManager.setBoolean(context, "is_driving", it)
//                    }
//                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
                SwitchCard(
                    title = "Tutorial",
                    desc = "Set tutorial as completed",
                    onCheckedChange = {
                        MyPreferenceManager.setBoolean(
                            context,
                            context.getString(R.string.tutorial_finished),
                            it
                        )
                        MyPreferenceManager.setInt(
                            context,
                            context.getString(R.string.count_once_met),
                            if (it) 1 else 0
                        )
                    },
                    isChecked = isTutorialFinished
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
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
//                Divider(
//                    color = MaterialTheme.colorScheme.outline
//                )
//                ButtonCard(
//                    title = "User ID",
//                    desc = "Change user ID",
//                    onAuthClick = {
//                        navController.navigate(AppRoutes.LOGIN)
//                    },
//                    buttonText = "Change"
//                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
                ButtonCard(
                    title = "Reset",
                    desc = "Reset to default",
                    onAuthClick = {
                        resetEverything(context)
                        Handler(Looper.getMainLooper()).post {
                            val toast =
                                Toast.makeText(context, "Reset to default!", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    },
                    buttonText = "Reset"
                )

            }

        }
    }
}


//fun newDayFunction(context: Context) {
//    val workRequestTest = PeriodicWorkRequestBuilder<MyDailyWorker>(15, TimeUnit.MINUTES)
//        .setInitialDelay(1000, TimeUnit.MILLISECONDS)
//        .build()
//    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//        "daily_maintenance_test",
//        ExistingPeriodicWorkPolicy.UPDATE, // KEEP or REPLACE
//        workRequestTest
//    )
////    val workRequestOneTime = OneTimeWorkRequestBuilder<MyDailyWorker>()
////        .setInitialDelay(1000, TimeUnit.MILLISECONDS)
////        .build()
////    WorkManager.getInstance(context).enqueue(workRequestOneTime)
//    Log.d("dayCount", "new day reset")
//}

fun resetTriggerFunction(context: Context) {
    val responseCounter = ResponseCounter(context)
    responseCounter.resetCounter()
    MyDelayManager.resetTriggerList()
}

fun testSurvey(context: Context) {
    val serviceIntent = Intent(context, OverlayService::class.java).apply {
        putExtra("tag", "count_test_met")
    }
    context.startService(serviceIntent)
}

