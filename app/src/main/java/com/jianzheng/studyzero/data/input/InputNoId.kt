package com.jianzheng.studyzero.data.input

data class InputNoId(
    val startTime: Long,
    val inputType: Int,
    val extraX: Int,
    val extraY: Int
) {
    companion object {
        fun fromInput(input: Input): InputNoId {
            return InputNoId(
                startTime = input.startTime,
                inputType = input.inputType,
                extraX = input.extraX,
                extraY = input.extraY
            )
        }
    }
}
