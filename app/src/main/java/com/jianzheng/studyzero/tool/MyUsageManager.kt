package com.jianzheng.studyzero.tool

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyUsageManager {
    fun getUsageEvents(context: Context, currentTime:Long) {

        val currentTimeString = getDateTime(currentTime)
        Log.d("usageManager","get usage events called @ $currentTimeString")
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val dayInMillis: Long = 24 * 60 * 60 * 1000L
        val dayInMillisTest: Long = 60 * 1000L
        val usageEvents = usageStatsManager.queryEvents(currentTime - dayInMillisTest, currentTime)
        val usageEvent = UsageEvents.Event()
//        val eventsList = mutableListOf<UsageEventModel>()

//        val gson = Gson()
//        val jsonData = gson.toJson(eventsList)
        val database = Firebase.database
        val myRef = database.getReference("Usage")
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val uid = sharedPreferences.getString("UID", "null")
        val dayCount = sharedPreferences.getInt("day_count", -1)
        if (sharedPreferences.getBoolean("UID_valid", false)) {//only upload for authenticated users
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                var i = 0
                Log.d("usageManager","day count is $dayCount")
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(usageEvent)
//            eventsList.add(UsageEventModel(usageEvent.packageName,usageEvent.timeStamp))
//            Log.d( "AppUsage", "${usageEvent.packageName} ${usageEvent.timeStamp}")
                    myRef.child(uid.toString()).child(dayCount.toString()).child(i.toString())
                        .child("app").setValue(usageEvent.packageName)
                    myRef.child(uid.toString()).child(dayCount.toString()).child(i.toString())
                        .child("time").setValue(getDateTime(usageEvent.timeStamp))
                    Log.d("usageManager","package name ${usageEvent.packageName}, time ${getDateTime(usageEvent.timeStamp)}")
                    i++
                }
                if (i == 0)
                    Log.d("usageManager","no usage to be upload")
            }
        }
        writeLocalFile() //write local files for all users
    }

    private fun writeLocalFile() {

    }
}

data class UsageEventModel(
    val packageName: String,
    val timeStamp: Long
)