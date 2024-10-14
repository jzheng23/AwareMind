package com.jianzheng.studyzero.data.response

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "response")
data class Response(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val delay: Long,
    val answer1: Int,
    val answer2: Int,
    val startingTime: Long,
    val submittingTime: Long
)
