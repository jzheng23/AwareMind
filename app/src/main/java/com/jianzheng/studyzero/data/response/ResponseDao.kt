package com.jianzheng.studyzero.data.response

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(response: Response)

    @Query("SELECT COUNT(id) from response")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(id) from response WHERE submittingTime > :todayInMillis")
    fun getCountToday(todayInMillis: Long): Flow<Int>

    @Query("SELECT * FROM response WHERE id = (SELECT MAX(id) FROM response)")
    fun getLastResponse(): Response

    @Query("SELECT * FROM response")
    suspend fun getAllOnce(): List<Response>

    @Query("SELECT * FROM response WHERE submittingTime >= :startOfYesterday AND submittingTime < :startOfToday")
    suspend fun getAllYesterday(startOfYesterday: Long, startOfToday: Long): List<Response>
}