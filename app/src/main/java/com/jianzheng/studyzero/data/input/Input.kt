package com.jianzheng.studyzero.data.input

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "input")
data class Input(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val inputType: Int,
    val extraX: Int,
    val extraY: Int
)
