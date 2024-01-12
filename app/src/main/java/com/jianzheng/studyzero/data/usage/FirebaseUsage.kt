package com.jianzheng.studyzero.data.usage

data class FirebaseUsage(
    val packageName: String,
    val className: String,
    val timeStamp: String,
)

fun Usage.toFirebaseUsage(): FirebaseUsage{
    return FirebaseUsage(
        packageName = packageName,
        className = className,
        timeStamp = timeStamp
    )
}
