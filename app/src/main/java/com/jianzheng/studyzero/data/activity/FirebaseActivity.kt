package com.jianzheng.studyzero.data.activity

data class FirebaseActivity(
    val startTime: Long,
    val activityType: Int
)

fun PhysicalActivity.toFirebaseActivity(): FirebaseActivity {
    return FirebaseActivity(
        startTime = startTime,
        activityType = activityType
    )
}