package com.jianzheng.studyzero.data.usage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Usage::class], version = 5, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}
