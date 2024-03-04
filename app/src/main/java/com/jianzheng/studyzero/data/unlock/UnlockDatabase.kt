package com.jianzheng.studyzero.data.unlock

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Unlock::class], version = 1, exportSchema = false)
abstract class UnlockDatabase : RoomDatabase() {
    abstract fun unlockDao(): UnlockDao
}