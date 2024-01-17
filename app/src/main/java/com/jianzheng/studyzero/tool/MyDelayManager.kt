package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.receiver.UnlockReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

object MyDelayManager {
    private val longTriggerList =
        mutableListOf(5, 8, 12, 18, 27, 41, 62, 95, 144, 220, 335, 510, 776)
    private val shortTriggerList = mutableListOf(1, 5, 8)
    private lateinit var originalTriggerList: MutableList<Int>
    private lateinit var triggerList: MutableList<Int>
    private var extraTriggerThirty = 30L //30 minutes for real 30 seconds for testing
    private var extraTriggerTwenty = 20L
    private var triggerIndex: Int = 0
    private lateinit var myWorkManager: WorkManager
    private lateinit var randomRequestID: UUID
    private lateinit var twentyRequestID: UUID
    private lateinit var thirtyRequestID: UUID
    private val unlockReceiver = UnlockReceiver()

    fun init(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        if (
            sharedPreferences.getBoolean("using_testing_triggers", true)
        ) {//testing
            originalTriggerList = shortTriggerList
            extraTriggerThirty = 30L //30 minutes for real 30 seconds for testing
            extraTriggerTwenty = 20L
        } else {//not testing
            originalTriggerList = longTriggerList
            extraTriggerThirty = 30L * 60 //30 minutes for real 30 seconds for testing
            extraTriggerTwenty = 20L * 60
        }
        triggerList = originalTriggerList.shuffled().toMutableList()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(unlockReceiver, filter)
        Log.d("testing", "init length of trigger list: ${triggerList.size}")
    }

    fun delayService(context: Context) {
        myWorkManager = WorkManager.getInstance(context)
        val responseCounter = ResponseCounter(context)
        val currentTimeMillis = System.currentTimeMillis()
        Log.d("delayTime", "passed in myDelayManager: $currentTimeMillis")
        if (!responseCounter.checkRandomComplete()) {
            val triggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
                .setInitialDelay(triggerList[triggerIndex].toLong(), TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        "tag" to context.getString(R.string.random_met),
                        "unlock" to currentTimeMillis
                    )
                )
                .build()
            myWorkManager.enqueue(triggerRequest)
            randomRequestID = triggerRequest.id
            Log.d(
                "unlock",
                "random trigger $triggerIndex is scheduled in ${triggerList[triggerIndex]} "
            )
            triggerIndex++
        }
        if (!responseCounter.check20Complete()) {
            val twentyTriggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
//                .setInitialDelay(extraTriggerTwenty, TimeUnit.MINUTES)
                .setInitialDelay(extraTriggerTwenty, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        "tag" to context.getString(R.string._20met),
                        "unlock" to currentTimeMillis
                    )
                )
                .build()
            myWorkManager.enqueue(twentyTriggerRequest)
            twentyRequestID = twentyTriggerRequest.id
            Log.d("unlock", "a fixed trigger is scheduled in 20 sec ")
        } else if (!responseCounter.check30Complete()) {
            val thirtyTriggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
//                .setInitialDelay(extraTriggerThirty, TimeUnit.MINUTES)
                .setInitialDelay(extraTriggerThirty, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        "tag" to context.getString(R.string._30met),
                        "unlock" to currentTimeMillis
                    )
                )
                .build()
            myWorkManager.enqueue(thirtyTriggerRequest)
            thirtyRequestID = thirtyTriggerRequest.id
            Log.d("unlock", "a fixed trigger is scheduled in 30 sec ")
        }
    }


    fun cancelScheduledService() {
        if (::myWorkManager.isInitialized) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                checkAndCancelRandomTrigger()
                if (::twentyRequestID.isInitialized) {
                    checkAndCancelFixedTrigger(twentyRequestID)
                }
                if (::thirtyRequestID.isInitialized) {
                    checkAndCancelFixedTrigger(thirtyRequestID)
                }
            }
        }
    }

    private fun checkAndCancelRandomTrigger() {
        if (::randomRequestID.isInitialized) {
            if (myWorkManager.getWorkInfoById(randomRequestID).get().state
                != WorkInfo.State.SUCCEEDED
            ) {
                myWorkManager.cancelWorkById(randomRequestID)
                Log.d("unlock", "a random trigger is canceled")
                recycleTrigger()
            }
        }
    }

    private fun checkAndCancelFixedTrigger(requestID: UUID) {
        if (myWorkManager.getWorkInfoById(requestID).get().state
            != WorkInfo.State.SUCCEEDED
        ) {
            myWorkManager.cancelWorkById(requestID)
            Log.d("unlock", "a fixed trigger is canceled")
        }
    }

    fun recycleTrigger() {
        if (triggerIndex > 0) {
            triggerList += triggerList[triggerIndex - 1]
            Log.d("unlock", "Trigger ${triggerList[triggerIndex - 1]} added to the list.")
        }
    }

    fun resetTriggerList() {
        triggerList = originalTriggerList.shuffled().toMutableList()
        Log.d("unlock", "trigger list is reset")
    }
}