package com.jianzheng.studyzero.data.response

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "response")
data class Response(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val answer: Int,
    val delay: Long,
    val startingTime: Long,
    val submittingTime: Long,
    val isSubmitted: Boolean = true,
    val dismissalType: String? = null
)