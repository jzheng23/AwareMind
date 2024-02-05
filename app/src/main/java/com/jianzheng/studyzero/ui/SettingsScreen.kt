package com.jianzheng.studyzero.ui

import android.app.AlertDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.jianzheng.studyzero.tool.MyPermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(AppRoutes.HOME)
                    }) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )

        },
        content = {
            ScrollSettingContent(it)
        }
    )
}

@Composable
fun ScrollSettingContent(it: PaddingValues) {
    val context = LocalContext.current
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
//    val batteryAllowed by usePollState {
//        MyPermissionManager.checkBatteryPermission(context)
//    }
    val allAllowed by usePollState {
        MyPermissionManager.checkAllPermissions(context)
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(it),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = mediumPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!notificationAllowed) {
                ArrowCard(
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
                ArrowCard(
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
                ArrowCard(
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

//            if (!batteryAllowed) {
//                ArrowCard(
//                    title = "Optimize battery usage",
//                    desc = "The app needs to be put into the whitelist, so" +
//                            " it won't killed by the system battery optimization.",
//                    onAuthClick = {
//                        MyPermissionManager.grantBatteryPermission(context)
//                    }
//                )
//            }

            if (!accessibilityAllowed) {
                ArrowCard(
                    title = "Accessibility",
                    desc = "To understand how you use your phone, the app needs to record taps, scrolls," +
                            " and text edits performed on the phone. For text edits, it will only record" +
                            "the number of characters you type, but not the content of the text. No " +
                            "personal or sensitive user data will be collected.",
                    onAuthClick = {
                        showAccessibilityConsentDialog(context) {
                            MyPermissionManager.grantAccessibilityPermission(context)
                        }
                    }
                )
            }
            if (allAllowed) {
                Text(
                    text = "All permissions have been allowed.",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun showAccessibilityConsentDialog(context: Context, onConsentGiven: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Accessibility Permission Required")
        .setMessage("To understand how you use your phone, this app needs to access accessibility services. This includes recording taps, scrolls, and the number of characters typed. No personal or sensitive data will be collected.")
        .setPositiveButton("Agree") { _, _ ->
            onConsentGiven()
        }
        .setNegativeButton("Cancel", null)
        .show()
}

