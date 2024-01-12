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
//        if (sharedPreferences.getBoolean("UID_valid", false)) {//only upload for authenticated users
//            val scope = CoroutineScope(Dispatchers.IO)
//            scope.launch {
//                var i = 0
//                Log.d("usageManager","day count is $dayCount")
//                while (usageEvents.hasNextEvent()) {
//                    usageEvents.getNextEvent(usageEvent)
////            eventsList.add(UsageEventModel(usageEvent.packageName,usageEvent.timeStamp))
//                    myRef.child(uid.toString()).child(dayCount.toString()).child(i.toString())
//                        .child("app").setValue(usageEvent.packageName)
//                    myRef.child(uid.toString()).child(dayCount.toString()).child(i.toString())
//                        .child("page").setValue(usageEvent.className)
//                    myRef.child(uid.toString()).child(dayCount.toString()).child(i.toString())
//                        .child("time").setValue(getDateTime(usageEvent.timeStamp))
//                    Log.d("usageManager","package name ${usageEvent.packageName}, time ${getDateTime(usageEvent.timeStamp)}")
//                    i++
//                }
//                if (i == 0)
//                    Log.d("usageManager","no usage to be upload")
//            }
//        }
        val db =
            Room.databaseBuilder(context, UsageDatabase::class.java,"usage_database")
                .fallbackToDestructiveMigration()
                .build()
        val usageDao = db.usageDao()
        val events = mutableListOf<UsageEvents.Event>()
        while (usageEvents?.hasNextEvent() == true) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            events.add(event)
        }

        // Use Coroutine for background processing
        CoroutineScope(Dispatchers.IO).launch {
            events.forEach { event ->
                val usage = Usage(
                    packageName = event.packageName,
                    className = event.className?: "",
                    timeStamp = getDateTime(event.timeStamp)
                )
                val generatedId = usageDao.insert(usage)
                if (sharedPreferences.getBoolean("UID_valid", false)) myRef.child(uid.toString()).child(generatedId.toString()).setValue(usage.copy(id = generatedId).toFirebaseUsage())
                Log.d("usageManager","${getEventTypeString(event.eventType)} ${event.className}, ${getDateTime(event.timeStamp)}")
            }
        }

    //writeLocalFile() //write local files for all users
    }

    private fun writeLocalFile() {

    }
}

data class UsageEventModel(
    val packageName: String,
    val timeStamp: Long
)

fun getEventTypeString(eventType: Int): String {
    return when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED"
        UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED"
        UsageEvents.Event.ACTIVITY_STOPPED -> "ACTIVITY_STOPPED"
        else -> "OTHERS"
    }
}