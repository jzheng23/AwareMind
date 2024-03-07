package com.jianzheng.studyzero

import android.app.Application
import androidx.room.Room
import com.jianzheng.studyzero.data.activity.ActivityDatabase
import com.jianzheng.studyzero.data.unlock.UnlockDatabase

class StudyZeroApp : Application() {

    lateinit var unlockDatabase: UnlockDatabase
    lateinit var activityDatabase: ActivityDatabase

    override fun onCreate() {
        super.onCreate()
        // Initialize the Room database
        unlockDatabase = Room.databaseBuilder(
            applicationContext,
            UnlockDatabase::class.java, "unlock-database"
        ).build()
        activityDatabase = Room.databaseBuilder(
            applicationContext,
            ActivityDatabase::class.java, "activity-database"
        ).build()
    }
}