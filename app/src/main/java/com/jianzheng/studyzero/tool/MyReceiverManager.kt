package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.jianzheng.studyzero.receiver.UnlockReceiver

object MyReceiverManager {
    private var unlockReceiver = UnlockReceiver()

    fun registerReceiver(context: Context) {
        if (!MyPreferenceManager.getBoolean(context,"receiver_registered")) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(unlockReceiver, filter)
            MyPreferenceManager.setBoolean(context, "receiver_registered",true)
        }
    }

    fun unregisterReceiver(context: Context) {
        if (MyPreferenceManager.getBoolean(context,"receiver_registered")) {
            context.unregisterReceiver(unlockReceiver)
            MyPreferenceManager.setBoolean(context, "receiver_registered",false)
        }
    }

}