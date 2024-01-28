package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.SharedPreferences

object MyPreferenceManager {

    private fun getSharedPreferences(context: Context): SharedPreferences {
//        return context.applicationContext.getSharedPreferences(
        return context.getSharedPreferences(
            "MySharedPrefs",
            Context.MODE_PRIVATE
        )
    }

    fun getInt(context: Context, key: String): Int =
        getSharedPreferences(context).getInt(key, 0)

    fun getString(context: Context, key: String): String =
        getSharedPreferences(context).getString(key, "") ?: ""

    fun getBoolean(context: Context, key: String): Boolean =
        getSharedPreferences(context).getBoolean(key, false)

    fun setInt(context: Context, key: String, value: Int) {
        getSharedPreferences(context).edit().apply {
            putInt(key, value)
            apply()
        }
    }

    fun setString(context: Context, key: String, value: String) {
        getSharedPreferences(context).edit().apply {
            putString(key, value)
            apply()
        }
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        getSharedPreferences(context).edit().apply {
            putBoolean(key, value)
            apply()
        }
    }

    fun getLong(context: Context, key: String):Long =
        getSharedPreferences(context).getLong(key, 0L)

    fun setLong(context: Context, key: String, value: Long) {
        getSharedPreferences(context).edit().apply {
            putLong(key, value)
            apply()
        }
    }
}
