package com.jianzheng.studyzero.data.unlock

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jianzheng.studyzero.data.activity.PhysicalActivity

@Dao
interface UnlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unlock: Unlock): Long

    @Query("SELECT * FROM unlock WHERE startTime >= :startOfYesterday AND startTime < :startOfToday")
    suspend fun getAllYesterday(startOfYesterday: Long, startOfToday: Long): List<Unlock>
}