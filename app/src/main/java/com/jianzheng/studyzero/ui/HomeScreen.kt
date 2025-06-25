package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.tool.MyPermissionManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.startEverything
import androidx.core.net.toUri
import com.jianzheng.studyzero.tool.stopEverything
import com.jianzheng.studyzero.ui.theme.StudyZeroEsmTheme

val mediumPadding = 10.dp
const val PASSWORD = "2358"


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
//    val isUserIdValid by sharedPrefsViewModel.isUserIdValid.observeAsState(false)
    val userAlias by sharedPrefsViewModel.userAlias.observeAsState("")
    val isTester by sharedPrefsViewModel.isTester.observeAsState(false)
    val cheatMode by sharedPrefsViewModel.cheatMode.observeAsState(false)
    val isTutorialFinished by sharedPrefsViewModel.isTutorialFinished.observeAsState(false)
    val isDataCollecting by sharedPrefsViewModel.isDataCollecting.observeAsState(false)
    if (userId == "") navController.navigate(AppRoutes.LOGIN)
    var clickOnUIDCount by remember { mutableIntStateOf(0) }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = if (validateUid(userId)) ("Hi, " + (if (userAlias == "") userId else userAlias) + "!") else "Invalid UID",
                    color = if (validateUid(userId)) MaterialTheme.colorScheme.onSurface else Color.Red,
                    modifier = Modifier.clickable {
                        clickOnUIDCount++
                        if (clickOnUIDCount > 2) navController.navigate(AppRoutes.LOGIN)
                    })
            },
        )
    }, content = {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(horizontal = mediumPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showPermissionCard) {
                    ArrowCard(
                        title = "Permission",
                        desc = "Grant permissions the app needs",
                        onAuthClick = { navController.navigate(AppRoutes.SETTINGS) })
                }
                if (!isTutorialFinished) {
                    ArrowCard(
                        title = "Tutorial",
                        desc = if (!showPermissionCard) "See what the survey is like and how it works" else "Please grant permissions to unlock the tutorial",
                        onAuthClick = {
                            if (!showPermissionCard) {
                                navController.navigate(AppRoutes.TUTORIAL)
                                MyPreferenceManager.setInt(
                                    context, context.getString(R.string.count_random_met), 0
                                )
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
                    WelcomeInfo(
                        sharedPrefsViewModel = sharedPrefsViewModel
                    ) {
                        navController.navigate(AppRoutes.EXPORT)
//                            Log.d("navigate", "to export")
                    }
                }
                if (isTester or cheatMode) {
                    DebugCard(
//                            userId = userId,
                    )
                }
            }
        }
    })
}


@Composable
fun WelcomeInfo(
    sharedPrefsViewModel: SharedPrefsViewModel? = null, onClickOfInput: () -> Unit = {}
) {
    val contextAsApp = LocalContext.current.applicationContext as Application
    val welcomeViewModel: WelcomeViewModel = viewModel(
        factory = WelcomeViewModelFactory(contextAsApp)
    )
    val todayCount = welcomeViewModel.todayCount.observeAsState()
    val totalCount = welcomeViewModel.totalCount.observeAsState()
    val dayCount = welcomeViewModel.dayCount.observeAsState()
    var clickOnInputCount by remember { mutableIntStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        InfoCard(
            dayCount = dayCount.value ?: 0,
            todayCount = todayCount.value ?: 0, totalCount = totalCount.value ?: 0
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Thanks for your input!",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.clickable {
                clickOnInputCount++
                if (clickOnInputCount > 2) onClickOfInput()
            })

        LogoAndContact(sharedPrefsViewModel = sharedPrefsViewModel)
    }

}


@Composable
fun LogoAndContact(sharedPrefsViewModel: SharedPrefsViewModel? = null) {
    val context = LocalContext.current
    var showCheatDialog by remember { mutableStateOf(false) }
    val url = stringResource(id = R.string.url_privacy_policy)
    var password by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
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
                .padding(end = mediumPadding)
        ) {
            val versionName = getVersionName(context)
            Text(
                text = stringResource(id = R.string.app_name) + " " + versionName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { showCheatDialog = true })
            if (showCheatDialog) {
                StudyZeroEsmTheme {
                    AlertDialog(onDismissRequest = {
                        showCheatDialog = false
                        password = ""
                    }, title = {
                        Text(
                            text = "Enter Password",
                                color = MaterialTheme.colorScheme.primary  // or secondary, error, etc.
                        )
                    }, text = {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,        // Text color when focused
                                    unfocusedTextColor = MaterialTheme.colorScheme.primary      // Text color when not focused
                                ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ), keyboardActions = KeyboardActions(
                                onDone = {
                                    if (password == PASSWORD) sharedPrefsViewModel?.setCheatMode(
                                        true
                                    )
                                }))
                    }, confirmButton = {
                        Button(onClick = {
                            if (password == PASSWORD) {
                                sharedPrefsViewModel?.setCheatMode(true)
                                showCheatDialog = false
                            }
                        }) {
                            Text("OK")
                        }
                    }, dismissButton = {
                        TextButton(onClick = {
                            showCheatDialog = false
                            password = ""
                        }) {
                            Text("Cancel")
                        }
                    })
                }
//                AlertDialog(onDismissRequest = {
//                    showCheatDialog = false
//                    password = ""
//                }, title = {
//                    Text(
//                        text = "Enter Password",
////                                color = MaterialTheme.colorScheme.primary  // or secondary, error, etc.
//                    )
//                }, text = {
//                    OutlinedTextField(
//                        value = password, onValueChange = { password = it },
////                                colors = OutlinedTextFieldDefaults.colors(
////                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,        // Text color when focused
////                                    unfocusedTextColor = MaterialTheme.colorScheme.primary      // Text color when not focused
////                                ),
//                        keyboardOptions = KeyboardOptions(
//                            imeAction = ImeAction.Done
//                        ), keyboardActions = KeyboardActions(
//                            onDone = {
//                                if (password == PASSWORD) sharedPrefsViewModel?.setCheatMode(
//                                    true
//                                )
//                            }))
//                }, confirmButton = {
//                    Button(onClick = {
//                        if (password == PASSWORD) {
//                            sharedPrefsViewModel?.setCheatMode(true)
//                            showCheatDialog = false
//                        }
//                    }) {
//                        Text("OK")
//                    }
//                }, dismissButton = {
//                    TextButton(onClick = {
//                        showCheatDialog = false
//                        password = ""
//                    }) {
//                        Text("Cancel")
//                    }
//                })

            }
            Row {
                Text("By", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Jian Zheng",
                    style = TextStyle(textDecoration = TextDecoration.Underline),
                    modifier = Modifier
                        .clickable {
                            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "message/rfc822" // MIME type for email
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("jzheng23@umd.edu"))
                                putExtra(Intent.EXTRA_SUBJECT, "From Study Zero")
                            }

                            if (emailIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(
                                    Intent.createChooser(
                                        emailIntent, "Choose an email client"
                                    )
                                )
//                                Log.d("email", "can do")
                            } else {
                                Log.d("email", "cannot do")
                            }
                        }
                        .padding(end = 2.dp))
            }

            Text(
                text = "Privacy Policy ",
                style = TextStyle(textDecoration = TextDecoration.Underline),
                textAlign = TextAlign.End,
                modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle the exception when no Activity can handle the Intent
                        }
                    })
        }
    }
}


@Composable
fun InfoCard(
    dayCount: Int,
    todayCount: Int,
    totalCount: Int
) {
    val context = LocalContext.current
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val isDataCollecting by usePollState {
        mutableStateOf(sharedPreferences.getBoolean("collecting_data", false))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

            RowOfInfo(intCount = dayCount, extraString = "Days")
            Spacer(modifier = Modifier.height(12.dp))
            if (dayCount > 14) {
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
            }
        }
    }

}

@Composable
fun RowOfInfo(
    intCount: Int = 0, extraString: String = ""
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
            text = extraString, style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview
@Composable
fun InfoPreview() {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = mediumPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoAndContact()
        }
    }
}


fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Not Found"
    } catch (e: PackageManager.NameNotFoundException) {
        "Not Found"
    }
}