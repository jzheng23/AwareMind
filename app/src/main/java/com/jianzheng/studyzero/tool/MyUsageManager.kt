package com.jianzheng.studyzero.tool

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.data.usage.Usage
import com.jianzheng.studyzero.data.usage.UsageDatabase
import com.jianzheng.studyzero.data.usage.toFirebaseUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyUsageManager {
    fun getUsageEvents(context: Context, currentTime: Long, test: Boolean = true) {

        val currentTimeString = getDateTime(currentTime)
        Log.d("usageManager", "get usage events called @ $currentTimeString")
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val dayInMillis: Long = 24 * 60 * 60 * 1000L
        val dayInMillisTest: Long = 60 * 1000L
        val rangeMillis = if (test) dayInMillisTest else dayInMillis
        val usageEvents = usageStatsManager.queryEvents(currentTime - rangeMillis, currentTime)
        val database = Firebase.database
        val myRef = database.getReference("Usage")
        //use MyPrefsManager
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val uid = sharedPreferences.getString("UID", "null")
        val db =
            Room.databaseBuilder(context, UsageDatabase::class.java, "usage_database")
                .fallbackToDestructiveMigration()
                .build()
        val usageDao = db.usageDao()
        val usages = mutableListOf<Usage>()
        val sessionStartTimes = mutableMapOf<Pair<String, String>, Long>()

        while (usageEvents?.hasNextEvent() == true) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            val key = Pair(event.packageName, event.className)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    sessionStartTimes[key] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    sessionStartTimes[key]?.let { startTime ->
                        val usage = Usage(
                            packageName = event.packageName,
                            className = event.className,
                            startTime = getDateTime(startTime),
                            endTime = getDateTime(event.timeStamp)
                        )
                        usages.add(usage)
                        sessionStartTimes.remove(key)
                    }
                }
            }
        }

        // Use Coroutine for background processing
        CoroutineScope(Dispatchers.IO).launch {
            usages.forEach { usage ->
                val generatedId = usageDao.insert(usage)
                if (sharedPreferences.getBoolean("is_UID_valid", false)) myRef.child(uid.toString())
                    .child(generatedId.toString())
                    .setValue(usage.copy(id = generatedId).toFirebaseUsage())
//                Log.d("usageManager","${getEventTypeString(event.eventType)} ${event.className}, ${getDateTime(event.timeStamp)}")
                Log.d("usageManager", usage.className + usage.startTime + usage.endTime)
            }
        }

    }

}
