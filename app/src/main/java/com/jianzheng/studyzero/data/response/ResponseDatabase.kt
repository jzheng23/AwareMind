package com.jianzheng.studyzero.data.response

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Response::class], version = 4, exportSchema = false)
abstract class ResponseDatabase : RoomDatabase() {
    abstract fun responseDao(): ResponseDao
}