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
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.startMyForegroundService
import com.jianzheng.studyzero.ui.AppNavigation
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            StudyZeroTheme {
                AppNavigation()
            }
        }
        auth = Firebase.auth
    }


    public override fun onStart() {
        super.onStart()
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Authentication", "signInAnonymously:success")
                } else {
                    Log.w("Authentication", "signInAnonymously:failure", task.exception)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (MyPreferenceManager.getBoolean(
                this, this.getString(R.string.collecting_data)
            )
        ) {
            startMyForegroundService(this)
        }

    }

}