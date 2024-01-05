package com.jianzheng.studyzero.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.jianzheng.studyzero.data.EsmDatabase
import com.jianzheng.studyzero.service.MyForegroundService
import com.jianzheng.studyzero.tool.MyPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

val mediumPadding = 12.dp


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier
) {
    val showDialog = remember { mutableStateOf(false) }
    val sharedPreferences =
        LocalContext.current.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val userId = remember {
        mutableStateOf(sharedPreferences.getString("UID", "null"))
    }
    val todayCount = remember {
        mutableStateOf(sharedPreferences.getInt("today_count", 0))
    }
    val totalCount = remember {
        mutableStateOf(sharedPreferences.getInt("total_count", 0))
    }
    if (showDialog.value) LoginPage(userId, showDialog)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userId.value == "null") {
            Text("Please login")
            LoginButton(
                showDialog = showDialog
            )
        } else {
            PermissionSetting(
                userId = userId,
                todayCount = todayCount,
                totalCount = totalCount
            )
        }
        TestingPage(
            userId = userId,
            modifier = modifier,
            showDialog = showDialog
        )
    }

}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PermissionSetting(
    userId: MutableState<String?>,
    todayCount: MutableState<Int>,
    totalCount: MutableState<Int>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current
        val notificationAllowed by usePollState {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val overlayAllowed by usePollState { Settings.canDrawOverlays(context) }
        val usageStatAllowed by usePollState {
            MyPermissionManager.checkUserStatPermission(context)
        }
        if (notificationAllowed and overlayAllowed and usageStatAllowed) {
            WelcomeInfo(
                userId = userId,
                todayCount = todayCount,
                totalCount = totalCount
            )
        } else {
            Text(
                text = "Permission setting",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }
        if (!notificationAllowed) {
            AuthCard(
                title = "Notification",
                desc = "Notifications keep the app alive.",
                onAuthClick = {
                    MyPermissionManager.grantNotificationPermission(context)
                },
            )
            Divider()
        }

        if (!overlayAllowed) {
            AuthCard(
                title = "Overlay",
                desc = "To show the survey without minimizing the current app.",
                onAuthClick = {
                    MyPermissionManager.grantOverlayPermission(context)
                },
            )
            Divider()
        }

        if (!usageStatAllowed) {
            AuthCard(
                title = "App usage",
                desc = "To access app usage history.",
                onAuthClick = {
                    MyPermissionManager.grantUserStatPermission(context)
                },
            )
        }

    }
}

@Composable
fun WelcomeInfo(
    userId: MutableState<String?>,
    todayCount: MutableState<Int>,
    totalCount: MutableState<Int>
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ){
        Text(
            text = "Welcome, ${userId.value}!",
            style = MaterialTheme.typography.headlineSmall
        )
        Divider()
        Text(
            text = "App is running.",
            style = MaterialTheme.typography.bodyLarge
        )
//        Text(
//            text = "App has been running for x day(s).",
//            style = MaterialTheme.typography.bodyLarge
//        )
//        Text(
//            text = "You have made:",
//            style = MaterialTheme.typography.bodyLarge,
//        )
//        Text(
//            text = "${todayCount.value} report(s) today,",
//            style = MaterialTheme.typography.bodyLarge,
//        )
//        Text(
//            text = "${totalCount.value} report(s) in total.",
//            style = MaterialTheme.typography.bodyLarge,
//        )

        Divider()
        Text(
            text = "Thanks for your input!",
            style = MaterialTheme.typography.headlineSmall,
        )
    }

}


@Composable
fun LoginButton(
    showDialog: MutableState<Boolean>
) {
    Button(
        onClick = {
            showDialog.value = true
        }
    ) {
        Text("Login")
    }
}


@Composable
fun AuthCard(
    title: String,
    desc: String,
    onAuthClick: () -> Unit,
    buttonText: String = "Allow"
) {
    Row(
        modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc, fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(onClick = onAuthClick) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun <T> usePollState(
    context: CoroutineContext = Dispatchers.Default,
    interval: Long = 1000L,
    getter: () -> T,
): MutableState<T> {
    val mutableState = remember { mutableStateOf(getter()) }
    LaunchedEffect(Unit) {
        withContext(context) {
            while (isActive) {
                delay(interval)
                mutableState.value = getter()
            }
        }
    }
    return mutableState
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