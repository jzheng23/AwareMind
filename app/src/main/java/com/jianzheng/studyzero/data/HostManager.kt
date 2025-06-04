package com.jianzheng.studyzero.data

import android.content.Context
import kotlin.random.Random
import androidx.core.content.edit

object HostManager {
    private const val PREF_NAME = "host_prefs"
    private const val KEY_HOST = "host"

    fun getHost(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var host = prefs.getString(KEY_HOST, null)

        if (host == null) {
            host = Random.nextInt(100000, 999999).toString()
            prefs.edit { putString(KEY_HOST, host) }
        }

        return host
    }
}