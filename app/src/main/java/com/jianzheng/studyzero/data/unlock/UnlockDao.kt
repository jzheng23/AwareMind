package com.jianzheng.studyzero.data.unlock

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface UnlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unlock: Unlock): Long
}