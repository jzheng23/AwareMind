package com.jianzheng.studyzero.data.response

data class ResponseNoId(
    val delay: Long,
    val answer: Int,
    val startingTime: Long,
    val submittingTime: Long
) {
    companion object {
        fun fromResponse(response: Response): ResponseNoId {
            return ResponseNoId(
                delay = response.delay,
                answer = response.answer,
                startingTime = response.startingTime,
                submittingTime = response.submittingTime
            )
        }
    }
}