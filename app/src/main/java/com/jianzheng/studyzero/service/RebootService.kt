package com.jianzheng.studyzero.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jianzheng.studyzero.tool.startEverything

class RebootService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startEverything(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
