package com.jianzheng.studyzero.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.StudyZeroApp
import com.jianzheng.studyzero.data.unlock.Unlock
import com.jianzheng.studyzero.data.unlock.toFirebaseUnlock
import com.jianzheng.studyzero.service.OverlayService
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.getDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                        Log.d("unlock", "unlocked: ${System.currentTimeMillis()}")
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
        val unlockRecord = Unlock(startTime = getDateTime(currentTime), isUnlock = isUnlock)
        val unlockDao = (context.applicationContext as StudyZeroApp).database.unlockDao()


        // Use an AsyncTask or a background thread to insert the record into the database
        // For Kotlin coroutines, you can launch a coroutine here if this method is called from a CoroutineScope
        CoroutineScope(Dispatchers.IO).launch {
            unlockDao.insert(unlockRecord)
            if (MyPreferenceManager.getBoolean(context, "is_UID_valid")) {
                val generatedId = unlockDao.insert(unlockRecord)
                uploadUnlockToFirebase(context, unlockRecord, generatedId)
            }
        }
    }

    private
    fun uploadUnlockToFirebase(context: Context, unlock: Unlock, generatedId: Long) {
        val uid = MyPreferenceManager.getString(context, context.getString(R.string.user_ID))
        val firebaseUnlock = unlock.toFirebaseUnlock()
        // Obtain a reference to your Firebase database
        val databaseReference = FirebaseDatabase.getInstance().getReference("Unlocks")

        // Create a unique key for each unlock event, or use a specific key if you want to overwrite existing data
        val key = databaseReference.push().key


        key?.let {
            databaseReference.child(uid).child(generatedId.toString()).setValue(firebaseUnlock)
                .addOnSuccessListener {
                    // Handle successful upload, e.g., logging or UI feedback
                    Log.d("Firebase", "Data successfully uploaded.")
                }
                .addOnFailureListener {
                    // Handle failed upload
                    Log.e("Firebase", "Failed to upload data.", it)
                }
        }
    }
}
