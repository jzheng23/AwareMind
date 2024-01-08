package com.jianzheng.studyzero.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SharedPrefsViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    private val _sharedPrefValue = MutableLiveData<Int>()
    val sharedPrefValue: LiveData<Int> = _sharedPrefValue

    init {
        // Initial load of data
        _sharedPrefValue.value = sharedPreferences.getInt("today_count", 0)
        Log.d("liveCounter", "View model init: today is ${_sharedPrefValue.value}")
        // Set up a listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "today_count") {
                _sharedPrefValue.value = sharedPreferences.getInt("today_count", 0)
                Log.d("liveCounter", "Change listener: today is ${_sharedPrefValue.value}")
            }
        }
    }
}

class SharedPrefsViewModelFactory(private val sharedPreferences: SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedPrefsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedPrefsViewModel(sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
