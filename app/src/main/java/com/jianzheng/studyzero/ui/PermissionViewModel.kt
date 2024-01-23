package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PermissionViewModel(application: Application) : AndroidViewModel(application) {
    private val _todayCount = MutableLiveData<Int>()
    val todayCount: LiveData<Int> = _todayCount
    private val sharedPreferences =
        application.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "today_count" -> _todayCount.value = sharedPreferences.getInt("today_count", 0)
            }
        }

    init {
        _todayCount.value = sharedPreferences.getInt("today_count", 0)
        // Set up a listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

class PermissionViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PermissionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PermissionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}