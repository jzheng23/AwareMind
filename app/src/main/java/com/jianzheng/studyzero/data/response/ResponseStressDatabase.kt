package com.jianzheng.studyzero.data.response

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ResponseStress::class], version = 1, exportSchema = false)
abstract class ResponseStressDatabase : RoomDatabase() {
    abstract fun responseStressDao(): ResponseStressDao
}
