package com.jianzheng.studyzero.data.response

data class EsmUiState(
    val answer: List<Int> = mutableListOf<Int>(),
    val isAnswerValid: Boolean
)

fun EsmUiState.toResponse(delay: Long, startingTime: Long): Response = Response(
    id = 0,
    delay = delay,
    answer1 = answer[0],
    answer2 = answer[1],
    startingTime = startingTime,
    submittingTime = System.currentTimeMillis()
)
