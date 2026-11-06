package com.jianzheng.studyzero.data.activity

import androidx.annotation.Keep

@Keep
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