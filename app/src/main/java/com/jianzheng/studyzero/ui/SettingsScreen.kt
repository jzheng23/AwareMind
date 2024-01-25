package com.jianzheng.studyzero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyPreferenceManager

@Composable
fun SettingsScreen(
    navController: NavController
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(mediumPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current
//        val sharedPreferences =
//            LocalContext.current.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
//        val userId = remember {
//            mutableStateOf(sharedPreferences.getString("UID", ""))
//        }
            val notificationAllowed by usePollState {
                MyPermissionManager.checkNotificationPermission(context)
            }
            val overlayAllowed by usePollState {
                MyPermissionManager.checkOverlayPermission(context)
            }
            val usageStatAllowed by usePollState {
                MyPermissionManager.checkUserStatPermission(context)
            }
            val accessibilityAllowed by usePollState {
                MyPermissionManager.checkAccessibilityPermission(context)
            }
            val batteryAllowed by usePollState {
                MyPermissionManager.checkBatteryPermission(context)
            }

            Text(
                text = "Permission setting",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            if (!notificationAllowed) {
                AuthCard(
                    title = "Notification",
                    desc = "Android system often kills apps running in the background. A persistent" +
                            " notification helps to keep the app alive. The app needs the notification" +
                            " permission to show a persistent notification.",
                    onAuthClick = {
                        MyPermissionManager.grantNotificationPermission(context)
                    },
                )
            }

            if (!overlayAllowed) {
                Divider()
                AuthCard(
                    title = "Overlay window",
                    desc = "The app displays the survey page in a overlay window, so once you have" +
                            " completed the survey, you can go directly" +
                            " back to whatever app you are using before the survey appears.",
                    onAuthClick = {
                        MyPermissionManager.grantOverlayPermission(context)
                    },
                )

            }

            if (!usageStatAllowed) {
                Divider()
                AuthCard(
                    title = "App usage",
                    desc = "To understand how you use your phone, The app needs to access app usage" +
                            " history. Only the app information (e.g., " +
                            "app names, like 'Twitter', and time stamps, like '2024-01-01 10:00:00:000') " +
                            "will be collected. No personal or sensitive user data will be collected.",
                    onAuthClick = {
                        MyPermissionManager.grantUserStatPermission(context)
                    },
                )
            }

            if (!batteryAllowed) {
                Divider()
                AuthCard(
                    title = "Optimize battery usage",
                    desc = "The app needs to be put into the whitelist, so" +
                            " it won't killed by the system battery optimization.",
                    onAuthClick = {
                        MyPermissionManager.grantBatteryPermission(context)
                    }
                )
            }

            if (!accessibilityAllowed) {
                Divider()
                AuthCard(
                    title = "Accessibility",
                    desc = "To understand how you use your phone, the app needs to record taps, scrolls," +
                            " and text edits performed on the phone. For text edits, it will only record" +
                            "the number of characters you type, but not the content of the text. No " +
                            "personal or sensitive user data will be collected.",
                    onAuthClick = {
                        MyPermissionManager.grantAccessibilityPermission(context)
                    }
                )
            }
            Button(
                onClick = {
                    navController.navigate(AppRoutes.HOME)
                },
            ) {
                Text("Home")
            }


        }
    }

}