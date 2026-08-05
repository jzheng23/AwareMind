package com.jianzheng.studyzero.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.DeviceInfo
import com.jianzheng.studyzero.data.HostManager
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController
) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome!") },
            )
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

            ) {
                Column(
                    modifier = Modifier
//                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = mediumPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArrowCard(
                        title = "Login",
                        desc = "Please login with the assigned user ID",
                        onAuthClick = {
                            showDialog = true
                        }
                    )
                }
                if (showDialog) {
                    Dialog(
                        onDismissRequest = {
                            showDialog = false
                        },
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),

                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                var userIdInput by remember { mutableStateOf("") }
                                var userAliasInput by remember {
                                    mutableStateOf("")
                                }
                                val context = LocalContext.current
                                OutlinedTextField(
                                    value = userIdInput,
                                    onValueChange = {
                                        userIdInput = it
//            MyPreferenceManager.setString(context, "user_ID", it)
                                    },
                                    label = { Text("User ID") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.semantics {
                                        contentDescription = "User ID"
                                    }
                                )
                                OutlinedTextField(
                                    value = userAliasInput,
                                    onValueChange = { userAliasInput = it },
                                    label = { Text("Alias (optional)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.semantics {
                                        contentDescription = "Alias"
                                    }
                                )
                                Button(
                                    onClick = {
                                        saveUserId(
                                            userIdInput,
                                            userAliasInput,
                                            context,
                                        )
                                        showDialog = false
                                        navController.navigate(AppRoutes.HOME)
                                    },
                                    enabled = (userIdInput != ""),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Confirm login" }
                                ) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}


fun saveUserId(
    userId: String,
    userAlias: String,
    context: Context
) {
    val userIdIsValidate: Boolean = validateUid(userId)
    val isTester = isTester(userId)
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putString(context.getString(R.string.user_ID), userId)
            .putString(context.getString(R.string.user_alias), userAlias)
            .putBoolean("UID_valid", userIdIsValidate)
            .putBoolean(context.getString(R.string.is_tester), isTester)
    }
    if (userIdIsValidate) {
        DeviceInfoTracker.saveTimezoneToFirebase(userId, context)
    }
//    Log.d("userId", "UID is $userId, and tester? $isTester")
}

fun isTester(userId: String?): Boolean {
    return userId?.startsWith("te", ignoreCase = true) ?: false
}

fun validateUid(userId: String?): Boolean {
    val pattern = "^[A-Za-z]{2}\\d{3}$"
    val regex = pattern.toRegex()
    return if (userId == null) {
        false
    } else {
        regex.matches(userId)
    }
}


object DeviceInfoTracker {
    private val database = FirebaseDatabase.getInstance()
    private val timezoneRef = database.getReference("timezones")
    private val deviceInfoRef = database.getReference("device_info")

    fun saveTimezoneToFirebase(userId: String, context: Context) {
        val host = HostManager.getHost(context)
        timezoneRef.child(userId).child(host).setValue(TimeZone.getDefault().id)
        deviceInfoRef.child(userId).child(host).setValue(DeviceInfo.getAllInfo())
    }
}
