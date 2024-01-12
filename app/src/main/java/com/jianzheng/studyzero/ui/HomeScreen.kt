package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.startEverything
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme

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

    val sharedPreferences =
        LocalContext.current.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val userId = remember {
        mutableStateOf(sharedPreferences.getString("UID", ""))
    }
    val isTester by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("is_tester", false))
    }
    if (showDialog.value) LoginPage(userId, showDialog)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userId.value == "") {
            Text("Please login")
            LoginButton(
                showDialog = showDialog
            )
        } else {
            if (showPermissionCard) {
                AuthCard(
                    title = "Permission",
                    desc = "Permission",
                    onAuthClick = { navController.navigate(AppRoutes.SETTINGS) }
                )
            }
            AuthCard(
                title = "Tutorial",
                desc = "Tutorial",
                onAuthClick = { navController.navigate(AppRoutes.TUTORIAL) },
                buttonText = "Start",
                buttonEnabled = !showPermissionCard
            )
            AuthCard(
                title = "Data collection",
                desc = "Start data collection",
                onAuthClick = { startEverything(context) },
                buttonText = "Start",
                buttonEnabled = !showPermissionCard
            )
            WelcomeInfo(userId, showDialog)

        }
        if (isTester.value) {
            DebugCard(
                userId = userId,
                modifier = modifier,
                showDialog = showDialog
            )
        }
    }
}


@Composable
fun ContactInfo() {
    Text("By jzheng23@umd.edu")
}

@Composable
fun UmdLogo() {
    Image(
        painterResource(id = R.drawable.umd_logo),
        contentDescription = null,
    )
}

@Composable
fun WelcomeInfo(
    userId: MutableState<String?> = mutableStateOf(""),
    showDialog: MutableState<Boolean>
) {
    val context = LocalContext.current.applicationContext as Application
    val sharedPrefsViewModel: SharedPrefsViewModel = viewModel(
        factory = SharedPrefsViewModelFactory(context)
    )
    val todayCount = sharedPrefsViewModel.todayCount.observeAsState()
    val totalCount = sharedPrefsViewModel.totalCount.observeAsState()
    val dayCount = sharedPrefsViewModel.dayCount.observeAsState()
    Log.d("liveCounter", "Compose: today is ${todayCount.value}")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome, ${userId.value}!",
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
        }
    ) {
        Text("Login")
    }
}


@Preview
@Composable
fun SettingPagePreview() {
    StudyZeroTheme(isOverlay = true) {
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
