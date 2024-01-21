package com.jianzheng.studyzero.ui

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.scheduleDailyMaintenance

@Composable
fun TutorialScreen2(
    navController: NavController
) {
    val context = LocalContext.current
    val scheduleButtonClicked = remember { mutableStateOf(false) }
    val randomMetCount by usePollState {
        mutableIntStateOf(MyPreferenceManager.getInt(context,"randomMet"))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tap the Schedule button to schedule a survey page to show after next unlock of the phone.")
        Button(
            onClick = {
                MyPreferenceManager.setBoolean(context, "using_once_triggers", true)
                MyDelayManager.init(context)
                Handler(Looper.getMainLooper()).post {
                    val toast = Toast.makeText(context, "Please lock and then unlock your phone.", Toast.LENGTH_SHORT)
                    toast.show()
                }
                scheduleButtonClicked.value = true
            },
            enabled = !scheduleButtonClicked.value
        ) {
            Text("Schedule")
        }
        Button(
            onClick = {
                navController.navigate(AppRoutes.HOME)
                MyPreferenceManager.setBoolean(context, "tutorial_finished", true)
                MyPreferenceManager.setBoolean(context, "using_once_triggers", false)
            },
            enabled = randomMetCount.intValue >= 1
        ) {
            Text("Done")
        }
    }

}