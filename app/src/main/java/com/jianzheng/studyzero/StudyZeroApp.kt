package com.jianzheng.studyzero

import android.app.Application
import androidx.room.Room
import com.jianzheng.studyzero.data.unlock.UnlockDatabase

class StudyZeroApp : Application() {

    lateinit var database: UnlockDatabase

    override fun onCreate() {
        super.onCreate()
        // Initialize the Room database
        database = Room.databaseBuilder(
            applicationContext,
            UnlockDatabase::class.java, "unlock-database"
        ).build()
    }
}