package com.jianzheng.studyzero.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Response::class], version = 2, exportSchema = false)
abstract class EsmDatabase : RoomDatabase() {
    abstract fun responseDao(): ResponseDao
}