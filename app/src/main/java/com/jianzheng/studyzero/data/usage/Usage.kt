package com.jianzheng.studyzero.data.usage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage")
data class Usage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
//    val className: String,
    val startTime: Long,
    val endTime: Long
)
