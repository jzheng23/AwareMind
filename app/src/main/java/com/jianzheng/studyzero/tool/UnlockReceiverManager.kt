package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.receiver.UnlockReceiver

object UnlockReceiverManager {
    private var unlockReceiver: UnlockReceiver? = null
    // REMOVED: registeredContext field entirely

    fun registerReceiver(context: Context) {
        if (unlockReceiver != null) {
            Log.w("UnlockReceiver", "Receiver already registered")
            return
        }

        unlockReceiver = UnlockReceiver()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        context.registerReceiver(unlockReceiver, filter)
        MyPreferenceManager.setBoolean(context, context.getString(R.string.unlock_registered), true)
    }

    fun unregisterReceiver(context: Context) {
        unlockReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
                unlockReceiver = null
                MyPreferenceManager.setBoolean(
                    context,
                    context.getString(R.string.unlock_registered),
                    false
                )
            } catch (e: IllegalArgumentException) {
                Log.e("UnlockReceiver", "Error unregistering receiver: ${e.message}")
            }
        }
    }
}