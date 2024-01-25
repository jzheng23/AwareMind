package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.startEverything
import com.jianzheng.studyzero.ui.theme.StudyZeroEsmTheme

val mediumPadding = 12.dp


@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val showDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val showPermissionCard by usePollState {
        !MyPermissionManager.checkAllPermissions(context)
    }
    val context2 = LocalContext.current.applicationContext as Application
    val sharedPrefsViewModel: SharedPrefsViewModel = viewModel(
        factory = SharedPrefsViewModelFactory(context2)
    )
    val userId by sharedPrefsViewModel.userId.observeAsState("")
    val isTester by sharedPrefsViewModel.isTester.observeAsState(false)
    val isTutorialFinished by sharedPrefsViewModel.isTutorialFinished.observeAsState(false)
    val isDataCollecting by sharedPrefsViewModel.isDataCollecting.observeAsState(false)

    if (showDialog.value) LoginPage(userId, showDialog)

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(mediumPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userId == "") {
                Text(
                    text = "Please login",
                    modifier = Modifier.semantics {
                        contentDescription = "Please login"
                    }
                )
                LoginButton(
                    showDialog = showDialog
                )
            } else {
                if (showPermissionCard) {
                    ButtonCard(
                        title = "Permission",
                        desc = "Grant permissions the app needs",
                        onAuthClick = { navController.navigate(AppRoutes.SETTINGS) },
                        buttonText = "Manage"
                    )
                }
                if (!isTutorialFinished) {
                    ButtonCard(
                        title = "Tutorial",
                        desc = if (!showPermissionCard) "See what the survey is like and how it works" else "Please grant permissions to unlock the tutorial",
                        onAuthClick = {
                            navController.navigate(AppRoutes.TUTORIAL)
                            MyPreferenceManager.setInt(context, "randomMet", 0)
                        },
                        buttonText = "Start",
                        buttonEnabled = !showPermissionCard
                    )
                }
                if (!isDataCollecting) {
                    ButtonCard(
                        title = "Data collection",
                        desc = if (isTutorialFinished) "Start data collection" else "Please complete tutorial first ",
                        onAuthClick = { startEverything(context) },
                        buttonText = "Start",
                        buttonEnabled = isTutorialFinished
                    )
                }
                if (isDataCollecting) {
                    WelcomeInfo(userId?:"", showDialog
                    ) {
                        navController.navigate(AppRoutes.EXPORT)
                        Log.d("navigate","to export")
                    }
                }

            }
            if (isTester) {
                DebugCard(
                    userId = userId,
                    modifier = modifier,
                )
            }
        }
    }

}


@Composable
fun ContactInfo() {
    Text("Jian Zheng, jzheng23@umd.edu")
}

@Composable
fun UmdLogo() {
    Image(
        painterResource(id = if (isSystemInDarkTheme()) R.drawable.logos_wordmark_light_text else R.drawable.logos_wordmark_dark_text),
        contentDescription = null,
    )
}

@Composable
fun WelcomeInfo(
    userId: String = "",
    showDialog: MutableState<Boolean>,
    onClickOfInput: () -> Unit
) {
    val context = LocalContext.current.applicationContext as Application
    val welcomeViewModel: WelcomeViewModel = viewModel(
        factory = WelcomeViewModelFactory(context)
    )
    val todayCount = welcomeViewModel.todayCount.observeAsState()
    val totalCount = welcomeViewModel.totalCount.observeAsState()
    val dayCount = welcomeViewModel.dayCount.observeAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome, $userId!",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable { showDialog.value = true }
        )
        Divider()
        Text(
            text = "App has been running for ${dayCount.value} day(s).",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "You have made:",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "${todayCount.value} report(s) today,",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "${totalCount.value} report(s) in total.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Divider()
        Text(
            text = "Thanks for your input!",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable { onClickOfInput() }
        )
        UmdLogo()
        ContactInfo()
    }

}


@Composable
fun LoginButton(
    showDialog: MutableState<Boolean>
) {
    Button(
        onClick = {
            showDialog.value = true
        },
        modifier = Modifier.semantics {
            contentDescription = "Login button"
        }
    ) {
        Text("Login")
    }
}


@Preview
@Composable
fun SettingPagePreview() {
    StudyZeroEsmTheme() {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.White.copy(alpha = 0.5f),
//            onClick = onClick
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                UmdLogo()
                ContactInfo()
            }
        }
    }
}
