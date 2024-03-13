package com.jianzheng.studyzero.data.unlock

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock")
data class Unlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val isUnlock: Boolean
)
