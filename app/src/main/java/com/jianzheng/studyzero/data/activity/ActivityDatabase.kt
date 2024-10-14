package com.jianzheng.studyzero.data.activity

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PhysicalActivity::class], version = 1, exportSchema = false)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}