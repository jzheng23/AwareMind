package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.jianzheng.studyzero.receiver.UnlockReceiver

object MyReceiverManager {
    private var unlockReceiver = UnlockReceiver()

    fun registerReceiver(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(unlockReceiver, filter)
        Log.d("unlock", "unlock receiver registered")
        MyPreferenceManager.setBoolean(context, "receiver_registered", true)
    }

    fun unregisterReceiver(context: Context) {
        if (MyPreferenceManager.getBoolean(context, "receiver_registered")) {
            context.unregisterReceiver(unlockReceiver)
            Log.d("unlock", "unlock receiver unregistered")
            MyPreferenceManager.setBoolean(context, "receiver_registered", false)
        } else Log.d("unlock", "unlock receiver is not registered")
    }
}