package com.jianzheng.studyzero.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jianzheng.studyzero.tool.MyPreferenceManager

@Preview
@Composable
fun LoginPage(
//    userId: MutableState<String?> = mutableStateOf(""),
    userId: String = "",
    userAlias: String = "",
    showDialog: MutableState<Boolean> = mutableStateOf(true),
    isTester: MutableState<Boolean> = mutableStateOf(true),
) {
    Dialog(
        onDismissRequest = {
            showDialog.value = false
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UseIdInputField(
                    userId = userId,
                    userAlias = userAlias,
                    showDialog = showDialog
                )
            }
        }
    }
}

fun saveUserId(
//    userId: MutableState<String?>,
    userId: String,
    userAlias: String,
//    isTester: MutableState<Boolean>,
    context: Context
) {
    val userIdIsValidate: Boolean = validateUid(userId)
    val isTester = isTester(userId)
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("UID", userId)
        .putString("user_alias", userAlias)
        .putBoolean("UID_valid", userIdIsValidate)
        .putBoolean("is_tester", isTester)
        .apply()
    Log.d("userId", "UID is $userId, and tester? ${isTester}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseIdInputField(
//    userId: MutableState<String?>,
    userId: String = "",
    userAlias: String = "",
    showDialog: MutableState<Boolean>
) {
    var userIdInput by remember { mutableStateOf(userId) }
    var userAliasInput by remember {
        mutableStateOf(userAlias)
    }
    val context = LocalContext.current
    userIdInput?.let { it ->
        OutlinedTextField(
            value = it,
            onValueChange = {
                userIdInput = it
                MyPreferenceManager.setString(context, "UID", it)
            },
            label = { Text("User ID") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
//            keyboardActions = KeyboardActions(
//                onDone = {
//                    showDialog.value = false
//                    saveUserId(
//                        userIdInput,
//                        isTester,
//                        context,
//                    )
//                }
//            )
        )
    }
    OutlinedTextField(
        value = userAliasInput,
        onValueChange = {userAliasInput = it},
        label = {Text("Alias (optional)")},
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
    )
    Button(
        onClick = {
            showDialog.value = false
            saveUserId(
                userIdInput,
                userAliasInput,
                context,
            )
        },
        enabled = (userIdInput != ""),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Confirm")
    }
}