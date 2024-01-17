package com.jianzheng.studyzero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.jianzheng.studyzero.tool.MyPermissionManager

@Composable
fun SettingsScreen(
    navController: NavController
) {
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
        Text(
            text = "Permission setting",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (!notificationAllowed) {
            AuthCard(
                title = "Notification",
                desc = "To keep the app alive.",
                onAuthClick = {
                    MyPermissionManager.grantNotificationPermission(context)
                },
            )
        }

        if (!overlayAllowed) {
            Divider()
            AuthCard(
                title = "Overlay",
                desc = "To show the survey.",
                onAuthClick = {
                    MyPermissionManager.grantOverlayPermission(context)
                },
            )

        }

        if (!usageStatAllowed) {
            Divider()
            AuthCard(
                title = "App usage",
                desc = "To access app usage history. Only the app information (e.g., app names and time stamps) will be collected. No personal or sensitive user data will be collected.",
                onAuthClick = {
                    MyPermissionManager.grantUserStatPermission(context)
                },
            )
        }

        if (!accessibilityAllowed) {
            Divider()
            AuthCard(
                title = "Accessibility",
                desc = "To record taps, scrolls, and text edits performed on the phone. No personal or sensitive user data will be collected.",
                onAuthClick = {
                    MyPermissionManager.grantAccessibilityPermission(context)
                }
            )
        }
    }
}