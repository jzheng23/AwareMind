package com.jianzheng.studyzero.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter

class DailyMaintenance : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        uploadData()
        resetTriggerList()
        return START_STICKY
    }
    private fun uploadData() {
        MyUsageManager.getUsageEvents(this)
    }

    private fun resetTriggerList() {
        val responseCounter = ResponseCounter(this)
        responseCounter.resetCounter()
        MyDelayManager.resetTriggerList()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}