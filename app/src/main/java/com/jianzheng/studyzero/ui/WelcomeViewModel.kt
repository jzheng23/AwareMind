package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _todayCount = MutableLiveData<Int>()
    private val _totalCount = MutableLiveData<Int>()
    private val _dayCount = MutableLiveData<Int>()
    val todayCount: LiveData<Int> = _todayCount
    val totalCount: LiveData<Int> = _totalCount
    val dayCount: LiveData<Int> = _dayCount
    private val sharedPreferences =
        application.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "count_today" -> _todayCount.value = sharedPreferences.getInt("count_today", 0)
                "count_total" -> _totalCount.value = sharedPreferences.getInt("count_total", 0)
                "count_day" -> _dayCount.value = sharedPreferences.getInt("count_day", 0)
            }
        }

    init {
        _todayCount.value = sharedPreferences.getInt("count_today", 0)
        _totalCount.value = sharedPreferences.getInt("count_total", 0)
        _dayCount.value = sharedPreferences.getInt("count_day", 0)
        // Set up a listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

class WelcomeViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WelcomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WelcomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

