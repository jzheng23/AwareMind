package com.jianzheng.studyzero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.UnlockReceiverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val testShown = remember {
        mutableStateOf(false)
    }
    val unlockScheduled = remember {
        mutableStateOf(false)
    }
    val onceMetCount by usePollState {
        MyPreferenceManager.getInt(context, context.getString(R.string.count_once_met))
    }
    if (onceMetCount > 0) navController.navigate(AppRoutes.HOME)
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Tutorial") }, navigationIcon = {
            IconButton(onClick = {
                navController.navigate(AppRoutes.HOME)
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "back"
                )
            }
        })
    }, content = {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = mediumPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onceMetCount == 0) {
                    EnhancedArrowCard(
                        title = "Test the Survey Page",
                        desc = if (testShown.value) "You can test again if you would like to." else "Please test and see the survey page.",
                        onAuthClick = {
                            testSurvey(context)
                            testShown.value = true
                        },
                        status = if (testShown.value) CardStatus.COMPLETED else CardStatus.PENDING
//                        status = CardStatus.COMPLETED

                    )
                    EnhancedArrowCard(
                        title = "Schedule a Survey",
                        desc = if (unlockScheduled.value) {
                            "Please lock and then unlock your phone."
                        } else if (testShown.value) {
                            "Schedule the survey page to appear after next unlock " + "of the phone. Please answer the questions. DO NOT dismiss the survey page."
                        } else {
                            "Please test the Survey Page first."
                        },
                        onAuthClick = {
                            if (testShown.value) {
                                MyPreferenceManager.setBoolean(
                                    context, "using_once_triggers", true
                                )
                                MyPreferenceManager.setInt(
                                    context, context.getString(R.string.count_once_met), 0
                                )
                                UnlockReceiverManager.registerReceiver(context)
                                MyDelayManager.init(context)
                                unlockScheduled.value = true
                            }
                        },
                        buttonEnabled = testShown.value,
                        status = if (unlockScheduled.value) CardStatus.WAITING else CardStatus.PENDING
//                        status = CardStatus.WAITING
                    )
                } else Text(
                    text = "Tutorial finished!", textAlign = TextAlign.Center
                )
            }

        }
    })
}

