package com.jianzheng.studyzero.data.input

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Input::class], version = 1, exportSchema = false)
abstract class InputDatabase : RoomDatabase() {
    abstract fun inputDao(): InputDao
}