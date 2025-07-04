package com.jianzheng.studyzero.data.usage

data class FirebaseUsage(
    val packageName: String,
//    val className: String,
    val startTime: Long,
    val endTime: Long
)

fun Usage.toFirebaseUsage(): FirebaseUsage {
    return FirebaseUsage(
        packageName = packageName,
//        className = className,
        startTime = startTime,
        endTime = endTime
    )
}
