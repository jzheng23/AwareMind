package com.jianzheng.studyzero.ui

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager

@Composable
fun TutorialScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val testShown = remember {
        mutableStateOf<Boolean>(false)
    }
    val onceMetCount by usePollState {
        MyPreferenceManager.getInt(context, "onceMet")
    }
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = mediumPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        navController.navigate(AppRoutes.HOME)
                        if (onceMetCount > 0) {
                            MyPreferenceManager.setBoolean(context, "tutorial_finished", true)
                            MyPreferenceManager.setBoolean(context, "using_once_triggers", false)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
//                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = "Tutorial",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }
            Divider(
                color = MaterialTheme.colorScheme.outline
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = mediumPadding, vertical = 4.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onceMetCount == 0) {
                    ArrowCard(
                        title = "Test the Survey Page",
                        desc = "Please test and see the survey page.",
                        onAuthClick = {
                            testSurvey(context)
                            testShown.value = true
                        }
                    )
                    ArrowCard(
                        title = "Schedule a Survey",
                        desc = if (testShown.value) "Tap the Schedule button and the survey page will appear after next unlock " +
                                "of the phone. Please answer the questions. DO NOT dismiss the survey page." else
                            "Please test the Survey Page first.",
                        onAuthClick = {
                            if (testShown.value) {
                                MyPreferenceManager.setBoolean(context, "using_once_triggers", true)
                                MyPreferenceManager.setInt(context, "randomMet", 0)
                                MyDelayManager.init(context)
                                Handler(Looper.getMainLooper()).post {
                                    val toast = Toast.makeText(
                                        context,
                                        "Please lock and then unlock your phone.",
                                        Toast.LENGTH_SHORT
                                    )
                                    toast.show()
                                }
                            }
                        },
                        buttonEnabled = testShown.value
                    )
                } else Text(
                    text = "Tutorial finished!",
                    style = MaterialTheme.typography.headlineSmall,
                    )
            }

        }
    }


}