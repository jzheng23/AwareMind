package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class PermissionViewModel(application: Application) : AndroidViewModel(application) {
    private val _todayCount = MutableLiveData<Int>()
    private val sharedPreferences =
        application.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "count_today" -> _todayCount.value = sharedPreferences.getInt("count_today", 0)
            }
        }

    init {
        _todayCount.value = sharedPreferences.getInt("count_today", 0)
        // Set up a listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

