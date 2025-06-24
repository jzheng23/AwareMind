package com.jianzheng.studyzero.data.response

data class DismissedEsm(
    val id: Long = 0,
    val delay: Long,
    val startingTime: Long,
    val submittingTime: Long,
    val dismissalType: String
)

// Extension function to convert to DismissedEsm
fun Response.toDismissedEsm(): DismissedEsm = DismissedEsm(
    id = this.id,
    delay = this.delay,
    startingTime = this.startingTime,
    submittingTime = this.submittingTime,
    dismissalType = this.dismissalType ?: "UNKNOWN"
)