package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyDelayManager

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> {
                if (context != null) {
                    val serviceIntent = Intent(context, OverlayService::class.java).apply {
                        putExtra("tag", "none")
                    }
                    context.stopService(serviceIntent)
                    Log.d("delayTime", "unlocked: ${System.currentTimeMillis()}")
                    MyDelayManager.delayService(context)
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                val serviceIntent = Intent(context, OverlayService::class.java).apply {
                    putExtra("tag", "none")
                }
                context?.stopService(serviceIntent)
                MyDelayManager.cancelScheduledService()
            }
        }
    }
}
