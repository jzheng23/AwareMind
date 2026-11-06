package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.receiver.UnlockReceiver

object UnlockReceiverManager {
    private var unlockReceiver: UnlockReceiver? = null

    /**
     * Always registers on the application context. Registering on an Activity ties the
     * receiver to that Activity, and Android drops it on rotation or theme change.
     */
    @Synchronized
    fun registerReceiver(context: Context) {
        val appContext = context.applicationContext
        if (unlockReceiver != null) {
            Log.w("UnlockReceiver", "Receiver already registered")
            return
        }

        val receiver = UnlockReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        ContextCompat.registerReceiver(
            appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        unlockReceiver = receiver
        MyPreferenceManager.setBoolean(appContext, appContext.getString(R.string.unlock_registered), true)
    }

    @Synchronized
    fun unregisterReceiver(context: Context) {
        val appContext = context.applicationContext
        unlockReceiver?.let { receiver ->
            try {
                appContext.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.e("UnlockReceiver", "Error unregistering receiver: ${e.message}")
            }
            unlockReceiver = null
            MyPreferenceManager.setBoolean(
                appContext,
                appContext.getString(R.string.unlock_registered),
                false
            )
        }
    }
}
