package com.jianzheng.studyzero.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SharedPrefsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences =
        application.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

    // LiveData for UID
    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    private val _userAlias = MutableLiveData<String>()
    val userAlias: LiveData<String> = _userAlias

    // LiveData for isTester
    private val _isTester = MutableLiveData<Boolean>()
    val isTester: LiveData<Boolean> = _isTester

    // LiveData for isTutorialFinished
    private val _isTutorialFinished = MutableLiveData<Boolean>()
    val isTutorialFinished: LiveData<Boolean> = _isTutorialFinished

    // LiveData for isDataCollecting
    private val _isDataCollecting = MutableLiveData<Boolean>()
    val isDataCollecting: LiveData<Boolean> = _isDataCollecting

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "UID" -> _userId.value = sharedPreferences.getString("UID", "")
                "user_alias" -> _userAlias.value = sharedPreferences.getString("user_alias", "")
                "is_tester" -> _isTester.value = sharedPreferences.getBoolean("is_tester", false)
                "tutorial_finished" -> _isTutorialFinished.value =
                    sharedPreferences.getBoolean("tutorial_finished", false)

                "collecting_data" -> _isDataCollecting.value =
                    sharedPreferences.getBoolean("collecting_data", false)
            }
        }

    init {
        // Initialize LiveData with current preferences values
        _userId.value = sharedPreferences.getString("UID", "")
        _userAlias.value = sharedPreferences.getString("user_alias", "")
        _isTester.value = sharedPreferences.getBoolean("is_tester", false)
        _isTutorialFinished.value = sharedPreferences.getBoolean("tutorial_finished", false)
        _isDataCollecting.value = sharedPreferences.getBoolean("collecting_data", false)

        // Register the listener for changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
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