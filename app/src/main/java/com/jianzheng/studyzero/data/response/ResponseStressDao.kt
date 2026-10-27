package com.jianzheng.studyzero.data.response

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponseStressDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(response: ResponseStress)

    @Query("SELECT COUNT(id) from response_stress")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(id) from response_stress WHERE submittingTime > :todayInMillis")
    fun getCountToday(todayInMillis: Long): Flow<Int>

    @Query("SELECT * FROM response_stress WHERE id = (SELECT MAX(id) FROM response_stress)")
    fun getLastResponse(): ResponseStress

    @Query("SELECT * FROM response_stress")
    suspend fun getAllOnce(): List<ResponseStress>

    @Query("SELECT * FROM response_stress WHERE submittingTime >= :startOfYesterday AND submittingTime < :startOfToday")
    suspend fun getAllYesterday(startOfYesterday: Long, startOfToday: Long): List<ResponseStress>
}
