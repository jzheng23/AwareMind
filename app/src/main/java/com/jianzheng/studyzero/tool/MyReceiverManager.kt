package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.jianzheng.studyzero.receiver.UnlockReceiver

object MyReceiverManager {
    private var isReceiverRegistered = false
    private var unlockReceiver = UnlockReceiver()

    fun registerReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(unlockReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun unregisterReceiver(context: Context) {
        if (isReceiverRegistered) {
            context.unregisterReceiver(unlockReceiver)
            isReceiverRegistered = false
        }
    }
}