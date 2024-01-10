package com.jianzheng.studyzero.tool

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getDateTime(timeInMillis: Long): String {
    val date = Date(timeInMillis)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    return formatter.format(date)
}