package com.jianzheng.studyzero.data.usage

data class FirebaseUsage(
    val packageName: String,
    val className: String,
    val startTime: String,
    val endTime: String
)

fun Usage.toFirebaseUsage(): FirebaseUsage {
    return FirebaseUsage(
        packageName = packageName,
        className = className,
        startTime = startTime,
        endTime = endTime
    )
}
