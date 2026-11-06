package com.jianzheng.studyzero.data.unlock

import androidx.annotation.Keep

@Keep
data class FirebaseUnlock(
    val startTime: Long,
    val isUnlock: Boolean
)

fun Unlock.toFirebaseUnlock(): FirebaseUnlock {
    return FirebaseUnlock(
        startTime = startTime,
        isUnlock = isUnlock
    )
}