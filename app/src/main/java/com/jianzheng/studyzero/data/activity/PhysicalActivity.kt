package com.jianzheng.studyzero.data.activity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity")
data class PhysicalActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val activityType: Int
)