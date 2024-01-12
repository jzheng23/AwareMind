package com.jianzheng.studyzero.data.usage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: Usage): Long
}