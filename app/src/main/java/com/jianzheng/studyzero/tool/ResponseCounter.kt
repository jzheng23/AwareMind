package com.jianzheng.studyzero.tool

import android.content.Context

class ResponseCounter(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()
    private val lengthOfTriggerList = if (
        sharedPreferences.getBoolean("using_testing_triggers", true)
    ) 3 else 13//13 for real, 3 for testing

    fun responsePlusOne(tag: String) {
        val retrievedValue = sharedPreferences.getInt(tag, 0)
        val todayCount = sharedPreferences.getInt("count_today", 0)
        val totalCount = sharedPreferences.getInt("count_total", 0)
        editor.putInt(tag, retrievedValue + 1)
            .putInt("count_today", todayCount + 1)
            .putInt("count_total", totalCount + 1)
            .apply()
        if (tag == "count_once_met") {
            editor.putBoolean("tutorial_finished", true)
                .putBoolean("using_once_triggers", false)
                .apply()
        }
//        Log.d("liveCounter", "Response counter: today is $todayCount, total is $totalCount")
    }

    fun check20Complete(): Boolean {
        return sharedPreferences.getInt("count_20_met", 0) >= 1
    }

    fun check30Complete(): Boolean {
        return sharedPreferences.getInt("count_30_met", 0) >= 2
    }

    fun checkRandomComplete(): Boolean {
//        Log.d("testing", "length of random trigger: $lengthOfTriggerList")
        return sharedPreferences.getInt("count_random_met", 0) >= lengthOfTriggerList
    }

    fun checkDailyLimitReached(): Boolean {
        return sharedPreferences.getInt("count_today", 0) >= 15
    }

//    fun checkComplete(): Boolean {
//        return (checkRandomComplete() and check20Complete() and check30Complete())
//    }

    fun resetCounter() {
        editor.putInt("count_20_Met", 0)
            .putInt("count_30_met", 0)
            .putInt("count_random_met", 0)
            .putInt("count_once_met", 0)
            .putInt("count_today", 0)
            .apply()
//        Log.d("unlock", "counter of responses is reset")
    }
}