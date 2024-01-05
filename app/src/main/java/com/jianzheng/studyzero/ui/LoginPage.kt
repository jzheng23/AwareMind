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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Preview
@Composable
fun LoginPage(
    userId: MutableState<String?> = mutableStateOf("null"),
    showDialog: MutableState<Boolean> = mutableStateOf(true)
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
                    userId,
                    showDialog
                )
            }
        }
    }
}

fun saveUserId(
    userId: MutableState<String?>,
    context: Context
) {
    val userIdIsValidate: Boolean = validateUid(userId.value)
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("UID", userId.value)
        .putBoolean("UID_valid", userIdIsValidate)
        .apply()
    Log.d("userId", "UID is ${userId.value}, and it is $userIdIsValidate")
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
    userId: MutableState<String?>,
    showDialog: MutableState<Boolean>
) {
    //var userId by remember { mutableStateOf("") }
    val context = LocalContext.current
    userId.value?.let { it ->
        TextField(
        value = it,
        onValueChange = { userId.value = it },
        label = { Text("User ID") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                showDialog.value = false
                saveUserId(
                    userId,
                    context
                )
            }
        )
    )
    }
    Button(
        onClick = {
            showDialog.value = false
            saveUserId(
                userId,
                context
            )
        }
    ) {
        Text("Confirm")
    }
}