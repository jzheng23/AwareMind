package com.jianzheng.studyzero.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jianzheng.studyzero.tool.startEverything

class RebootService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Your code to be executed after reboot
        // ...

        // If the service is not meant to be sticky
        startEverything(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // Return null as this is not a bound service
        return null
    }

}
