package com.jianzheng.studyzero.data.input

import androidx.annotation.Keep

@Keep
data class FirebaseInput(
    val startTime: Long,
    val inputType: Int,
    val extraX: Int,
    val extraY: Int
)

fun Input.toFirebaseInput(): FirebaseInput {
    return FirebaseInput(
        startTime = startTime,
        inputType = inputType,
        extraX = extraX,
        extraY = extraY
    )
}