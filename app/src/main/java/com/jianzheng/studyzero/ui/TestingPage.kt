package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.ToggleButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter

@Composable
fun TestingPage(
    userId: MutableState<String?>,
    modifier: Modifier,
    showDialog: MutableState<Boolean>
) {
    val context = LocalContext.current
    Card(
//        modifier = Modifier.padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(mediumPadding)
        ) {
            Text(
                text = "Only for testing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text("User ID is ${userId.value}, upload is ${validateUid(userId.value)}")
            AuthCard(
                title = "ESM",
                desc = "Test the ESM page",
                onAuthClick = {
                    testFunction(context)
                },
                buttonText = "Test"
            )
            Divider()
            SwitchCard(
                title = "Trigger list for testing",
                desc = "Using triggers for testing (shorter and fewer)?",
            )
            Divider()
            AuthCard(
                title = "Triggers",
                desc = "Reset triggers",
                onAuthClick = {
                    resetTriggerFunction(context)
                },
                buttonText = "Reset"
            )
            Divider()
            AuthCard(
                title = "User ID",
                desc = "Change User ID",
                onAuthClick = {
                    showDialog.value = true
                },
                buttonText = "Change"
            )
            Divider()
            AuthCard(
                title = "Usage",
                desc = "Get usage in last 10 seconds",
                onAuthClick = {
                    MyUsageManager.getUsageEvents(context)
                },
                buttonText = "Get"
            )
        }
    }
}

fun resetTriggerFunction(context: Context) {
    val responseCounter = ResponseCounter(context)
    responseCounter.resetCounter()
    MyDelayManager.resetTriggerList()
    Handler(Looper.getMainLooper()).post {
        val toast = Toast.makeText(context, "Triggers reset!", Toast.LENGTH_SHORT)
        toast.show()
    }
}

fun testFunction(context: Context) {
    val serviceIntent = Intent(context, OverlayService::class.java).apply {
        putExtra("tag", "testMet")
    }
    context.startService(serviceIntent)
}

@Composable
fun SwitchCard(
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc, fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        SwitchMinimalExample()
    }
}

@Composable
fun SwitchMinimalExample(
) {
    var checked by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    Switch(
        checked = checked,
        onCheckedChange = {
            checked = it
            editor.putBoolean("using_testing_triggers", it)
                .apply()
            MyDelayManager.init(context)
            resetTriggerFunction(context)
            Log.d("testing", "testing is $it")
        }
    )
}