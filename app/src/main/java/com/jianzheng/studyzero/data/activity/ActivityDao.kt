package com.jianzheng.studyzero.data.activity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: PhysicalActivity)

    @Query("SELECT * FROM activity WHERE startTime >= :startOfYesterday AND startTime < :startOfToday")
    suspend fun getAllYesterday(startOfYesterday: Long, startOfToday: Long): List<PhysicalActivity>
}