package com.jianzheng.studyzero.data

data class ResponseNoId(
    val delay: Long,
    val answer1: Int,
    val answer2: Int,
    val startingTime: String,
    val submittingTime: String
) {
    companion object {
        fun fromResponse(response: Response): ResponseNoId {
            return ResponseNoId(
                delay = response.delay,
                answer1 = response.answer1,
                answer2 = response.answer2,
                startingTime = response.startingTime,
                submittingTime = response.submittingTime
            )
        }
    }
}