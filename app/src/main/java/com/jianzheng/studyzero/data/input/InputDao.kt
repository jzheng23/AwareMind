package com.jianzheng.studyzero.data.input

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(input: Input): Long

    @Query("SELECT * FROM input WHERE startTime >= :startOfYesterday AND startTime < :startOfToday")
    suspend fun getAllYesterday(startOfYesterday: Long, startOfToday: Long): List<Input>
}