package com.jianzheng.studyzero.data.activity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

// Uploaded as-is to Firebase by the daily batch.
@Keep
@Entity(tableName = "activity")
data class PhysicalActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val activityType: Int
)