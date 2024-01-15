package com.jianzheng.studyzero

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jianzheng.studyzero.receiver.UnlockReceiver
import com.jianzheng.studyzero.service.MyAccessibilityService
import com.jianzheng.studyzero.ui.AppNavigation

class MainActivity : ComponentActivity() {
    private val unlockReceiver = UnlockReceiver()
    private lateinit var auth: FirebaseAuth


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavigation()  // This sets up the entire navigation for your app
        }
        auth = Firebase.auth
        registerReceiver(unlockReceiver,)

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInAnonymously:success")
                    val user = auth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Authentication", "signInAnonymously:failure", task.exception)
//                    Toast.makeText(
//                        baseContext,
//                        "Authentication failed.",
//                        Toast.LENGTH_SHORT,
//                    ).show()
                }
            }
    }

}