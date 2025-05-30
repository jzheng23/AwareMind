package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.google.firebase.database.FirebaseDatabase
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.unlock.Unlock
import com.jianzheng.studyzero.data.unlock.UnlockDatabase
import com.jianzheng.studyzero.data.unlock.toFirebaseUnlock
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.ui.validateUid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> {
                if (context != null) {
                    if (MyPreferenceManager.getBoolean(
                            context,
                            context.getString(R.string.unlock_registered)
                        )
                    ) {
                        val serviceIntent = Intent(context, OverlayService::class.java).apply {
                            putExtra("tag", "none")
                        }
                        context.stopService(serviceIntent)
//                        Log.d("unlock", "unlocked: ${System.currentTimeMillis()}")
                        MyDelayManager.scheduleOverlayService(context)
                        logUnlockOrLock(context, true)
                    } else Log.d("unlock", "receiver not registered")
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                val serviceIntent = Intent(context, OverlayService::class.java).apply {
                    putExtra("tag", "none")
                }
                context?.stopService(serviceIntent)
                MyDelayManager.cancelScheduledService()
                if (context != null) {
                    logUnlockOrLock(context, false)
                }
            }
        }
    }

    private fun logUnlockOrLock(context: Context, isUnlock: Boolean) {
        val currentTime = System.currentTimeMillis()
        val uid = MyPreferenceManager.getString(context, context.getString(R.string.user_ID))
        val unlockRecord = Unlock(startTime = currentTime, isUnlock = isUnlock)
        val unlockDao = Room.databaseBuilder(context, UnlockDatabase::class.java, "unlock_database")
            .fallbackToDestructiveMigration(false)
            .build()
            .unlockDao()


        // Use an AsyncTask or a background thread to insert the record into the database
        // For Kotlin coroutines, you can launch a coroutine here if this method is called from a CoroutineScope
        CoroutineScope(Dispatchers.IO).launch {
            val generatedId = unlockDao.insert(unlockRecord)
            if (validateUid(uid)) {
                uploadUnlockToFirebase(context, unlockRecord, generatedId)
            }
        }
    }

    private
    fun uploadUnlockToFirebase(context: Context, unlock: Unlock, generatedId: Long) {
        val uid = MyPreferenceManager.getString(context, context.getString(R.string.user_ID))
        val todayDate = LocalDate.now().toString()
        val firebaseUnlock = unlock.toFirebaseUnlock()
        // Obtain a reference to your Firebase database
        val databaseReference = FirebaseDatabase.getInstance().getReference("Unlock")

        // Create a unique key for each unlock event, or use a specific key if you want to overwrite existing data
        val key = databaseReference.push().key


        key?.let {
            databaseReference.child(uid)
                .child(todayDate)
                .child(generatedId.toString())
                .setValue(firebaseUnlock)
        }
    }
}
