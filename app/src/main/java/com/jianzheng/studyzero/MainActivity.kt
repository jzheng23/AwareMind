package com.jianzheng.studyzero

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jianzheng.studyzero.tool.MyReceiverManager
import com.jianzheng.studyzero.ui.AppNavigation
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            StudyZeroTheme {
                AppNavigation()
            }
        }
        auth = Firebase.auth
    }

    override fun onDestroy() {
        super.onDestroy()
        MyReceiverManager.unregisterReceiver(this)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInAnonymously:success")
//                    val user = auth.currentUser
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