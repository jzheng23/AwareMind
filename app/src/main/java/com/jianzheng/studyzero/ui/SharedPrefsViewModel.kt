package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SharedPrefsViewModel(application: Application) : AndroidViewModel(application) {
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
                "today_count" -> _todayCount.value = sharedPreferences.getInt("today_count", 0)
                "total_count" -> _totalCount.value = sharedPreferences.getInt("total_count", 0)
                "day_count" -> _dayCount.value = sharedPreferences.getInt("day_count", 0)
            }
        }

    init {

        _todayCount.value = sharedPreferences.getInt("today_count", 0)
        _totalCount.value = sharedPreferences.getInt("total_count", 0)
        _dayCount.value = sharedPreferences.getInt("day_count", 0)
        // Set up a listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

class SharedPrefsViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedPrefsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedPrefsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

