package com.jianzheng.studyzero.data

import com.jianzheng.studyzero.tool.getDateTime

data class EsmUiState(
    val answer: List<Int> = mutableListOf<Int>(),
    val isAnswerValid: Boolean
)

fun EsmUiState.toResponse(delay: Long, startingTime: Long): Response = Response(
    id = 0,
    delay = delay,
    answer1 = answer[0],
    answer2 = answer[1],
    startingTime = getDateTime(startingTime),
    submittingTime = getDateTime(System.currentTimeMillis())
)
