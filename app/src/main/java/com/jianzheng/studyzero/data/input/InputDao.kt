package com.jianzheng.studyzero.data.input

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface InputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(input: Input): Long
}