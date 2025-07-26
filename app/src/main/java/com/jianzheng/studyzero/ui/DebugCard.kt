package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.CardDefaults
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
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.resetEverything
import com.jianzheng.studyzero.tool.startEverything
import com.jianzheng.studyzero.tool.stopEverything

@Composable
fun DebugCard(
//    navController: NavController,
//    userId: String = "",
) {
    var isVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val isOverlayAllowed by remember {
        mutableStateOf(MyPermissionManager.checkOverlayPermission(context))
    }
//    val isUsageAllowed by remember {
//        mutableStateOf(MyPermissionManager.checkUserStatPermission(context))
//    }
//    val isA11yAllowed by remember {
//        mutableStateOf(MyPermissionManager.checkAccessibilityPermission(context))
//    }
//    val isInputRecording by usePollState {
//        sharedPreferences.getBoolean("input_record", false)
//    }
    val lastMaintenance by usePollState(interval = 3_600_000L) {
        sharedPreferences.getString("last_maintenance", "unknown")
    }
    val isDataCollecting by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("collecting_data", false))
    }

    val currentActivity by usePollState {
        mutableStateOf(sharedPreferences.getString("current_activity", "unknown"))
    }

    Card(
//        modifier = Modifier.padding(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 4.dp),

        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

                Text("Current activity: ${currentActivity.value}")
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
                Text("Synced at: $lastMaintenance")

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )

                ButtonCard(
                    title = "", desc = "Test the ESM page", onAuthClick = {
                        testSurvey(context)
                    }, buttonText = "Test", buttonEnabled = isOverlayAllowed
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

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
                ButtonCard(
                    title = "Reset", desc = "Reset to default", onAuthClick = {
                        resetEverything(context)

                    }, buttonText = "Reset"
                )

            }

        }
    }
}


fun testSurvey(context: Context) {
    val serviceIntent = Intent(context, OverlayService::class.java).apply {
        putExtra("tag", "count_test_met")
    }
    context.startService(serviceIntent)
}

