package com.jianzheng.studyzero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun TutorialScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val testShown = remember {
        mutableStateOf<Boolean>(false)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please tap the Test button below to see the survey page.")
        Button(
            onClick = {
                testSurvey(context)
                testShown.value = true
            }
        ) {
            Text("Test")
        }
        Button(
            onClick = {
                navController.navigate(AppRoutes.TUTORIAL2)
            },
            enabled = testShown.value
        ) {
            Text("Next")
        }
    }

}