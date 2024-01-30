package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.startEverything

val mediumPadding = 12.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val showPermissionCard by usePollState {
        !MyPermissionManager.checkAllPermissions(context)
    }
    val context2 = LocalContext.current.applicationContext as Application
    val sharedPrefsViewModel: SharedPrefsViewModel = viewModel(
        factory = SharedPrefsViewModelFactory(context2)
    )
    val userId by sharedPrefsViewModel.userId.observeAsState("")
    val userAlias by sharedPrefsViewModel.userAlias.observeAsState("")
    val isTester by sharedPrefsViewModel.isTester.observeAsState(false)
    val isTutorialFinished by sharedPrefsViewModel.isTutorialFinished.observeAsState(false)
    val isDataCollecting by sharedPrefsViewModel.isDataCollecting.observeAsState(false)

    if (userId == "") navController.navigate(AppRoutes.LOGIN)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Hi, " + if (userAlias == "") userId else userAlias)
                },
            )
        },
        content = {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showPermissionCard) {
                        ArrowCard(
                            title = "Permission",
                            desc = "Grant permissions the app needs",
                            onAuthClick = { navController.navigate(AppRoutes.SETTINGS) }
                        )
                    }
                    if (!isTutorialFinished) {
                        ArrowCard(
                            title = "Tutorial",
                            desc = if (!showPermissionCard) "See what the survey is like and how it works" else "Please grant permissions to unlock the tutorial",
                            onAuthClick = {
                                if (!showPermissionCard) {
                                    navController.navigate(AppRoutes.TUTORIAL)
                                    MyPreferenceManager.setInt(context, "randomMet", 0)
                                }
                            },
                            buttonEnabled = !showPermissionCard
                        )
                    }
                    if (!isDataCollecting) {
                        ArrowCard(
                            title = "Data collection",
                            desc = if (isTutorialFinished) "Start data collection" else "Please complete tutorial first ",
                            onAuthClick = {
                                if (isTutorialFinished) {
                                    startEverything(context)
                                }
                            },
                            buttonEnabled = isTutorialFinished
                        )
                    }
                    if (isDataCollecting) {
                        WelcomeInfo {
                            navController.navigate(AppRoutes.EXPORT)
                            Log.d("navigate", "to export")
                        }
                    }
                    if (isTester) {
                        DebugCard(
                            navController,
                            userId = userId,

                            )
                    }
                }
            }
        }
    )
}


@Composable
fun WelcomeInfo(
    onClickOfInput: () -> Unit = {}
) {
//    val todayCount = remember { mutableIntStateOf(0) }
//    val totalCount = remember { mutableIntStateOf(0) }
//    val dayCount = remember { mutableIntStateOf(0) }


    val contextAsApp = LocalContext.current.applicationContext as Application
    val welcomeViewModel: WelcomeViewModel = viewModel(
        factory = WelcomeViewModelFactory(contextAsApp)
    )
    val todayCount = welcomeViewModel.todayCount.observeAsState()
    val totalCount = welcomeViewModel.totalCount.observeAsState()
    var clickOnInputCount by remember { mutableIntStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        InfoCard(
//            dayCount = dayCount.value ?: 0,
            todayCount = todayCount.value ?: 0,
            totalCount = totalCount.value ?: 0
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Thanks for your input!",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable {
                clickOnInputCount++
                if (clickOnInputCount > 2) onClickOfInput()
            }
        )

        LogoAndContact()
    }

}

@Composable
fun LogoAndContact() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 12.dp)
    ) {
        Image(
            painterResource(id = if (isSystemInDarkTheme()) R.drawable.logos_wordmark_light_text else R.drawable.logos_wordmark_dark_text),
            contentDescription = null,
            modifier = Modifier
                .weight(1f) // The weight is a ratio, so both use 1f to split space equally
                .fillMaxWidth()
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(1f) // The weight is a ratio, so both use 1f to split space equally
                .fillMaxWidth()
        ) {
            Text("Jian Zheng")
            Text(
                text = "jzheng23@umd.edu",
                modifier = Modifier.clickable {
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822" // MIME type for email
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("jzheng23@umd.edu"))
                        putExtra(Intent.EXTRA_SUBJECT, "From Study Zero")
                    }

                    if (emailIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(
                            Intent.createChooser(
                                emailIntent,
                                "Choose an email client"
                            )
                        )
                        Log.d("email", "can do")
                    } else {
                        Log.d("email", "cannot do")
                    }
                }
            )
        }
    }
}


@Composable
fun InfoCard(
    todayCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
//        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)

        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RowOfInfo(intCount = todayCount, extraString = "Today")
            RowOfInfo(intCount = totalCount, extraString = "In total")
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

}

@Composable
fun RowOfInfo(
    intCount: Int = 0,
    extraString: String = ""
) {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = intCount.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = extraString,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


