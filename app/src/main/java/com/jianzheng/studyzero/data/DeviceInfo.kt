package com.jianzheng.studyzero.data

import android.os.Build

object DeviceInfo {
    fun getAllInfo(): Map<String, String> = mapOf(
        "Model" to Build.MODEL,
        "Brand" to Build.BRAND,
        "Base" to Build.VERSION.RELEASE
    )
}