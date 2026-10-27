package com.jianzheng.studyzero.data.response

data class ResponseStressNoId(
    val delay: Long,
    val answer: Int,
    val startingTime: Long,
    val submittingTime: Long
) {
    companion object {
        fun fromResponseStress(response: ResponseStress): ResponseStressNoId {
            return ResponseStressNoId(
                delay = response.delay,
                answer = response.answer,
                startingTime = response.startingTime,
                submittingTime = response.submittingTime
            )
        }
    }
}
