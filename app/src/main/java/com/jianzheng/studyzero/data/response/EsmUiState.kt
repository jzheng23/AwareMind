package com.jianzheng.studyzero.data.response

data class EsmUiState(
    val answer: Int = 0,
    val isAnswerValid: Boolean = false,
    val dismissalType: DismissalType? = null
)

enum class DismissalType {
    MANUAL,
    AUTO
}

fun EsmUiState.toResponse(delay: Long, startingTime: Long): Response = Response(
    id = 0,
    delay = delay,
    answer = answer,
    startingTime = startingTime,
    submittingTime = System.currentTimeMillis()
)
