package com.jianzheng.studyzero.data.response

data class EsmUiState(
    val answer: Int,
    val isAnswerValid: Boolean
)

fun EsmUiState.toResponse(delay: Long, startingTime: Long): Response = Response(
    id = 0,
    delay = delay,
    answer = answer,
    startingTime = startingTime,
    submittingTime = System.currentTimeMillis()
)
