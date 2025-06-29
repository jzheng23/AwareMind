package com.jianzheng.studyzero.tool

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jianzheng.studyzero.R
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
    private var tempTag: String = "count_random_met"
    private var countDelayManager = 0


    fun init(context: Context) {
        countDelayManager++
//        Log.d("unlock", "My delay manager initialized $countDelayManager")
        if (MyPreferenceManager.getBoolean(
                context,
                context.getString(R.string.using_once_triggers)
            )
        ) {
            originalTriggerList = mutableListOf(1)
            extraTriggerThirty = 30L * 60 //30 minutes for real 30 seconds for testing
            extraTriggerTwenty = 20L * 60
            tempTag = "count_once_met"
        } else if (
            MyPreferenceManager.getBoolean(
                context,
                context.getString(R.string.using_testing_triggers)
            )
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

//        Log.d("unlock", "init length of trigger list: ${triggerList.size}")
    }


    fun scheduleOverlayService(context: Context) {

        myWorkManager = WorkManager.getInstance(context)
        val responseCounter = ResponseCounter(context)
//        Log.d(
//            "unlock",
//            "TriggerIndex: $triggerIndex, triggerListSize: ${triggerList.size}, random completed: ${responseCounter.checkRandomComplete()}"
//        )
        if (responseCounter.checkDailyLimitReached()) {
            return
        }
        val currentTimeMillis = System.currentTimeMillis()
        val lastSubmit =
            MyPreferenceManager.getLong(context, context.getString(R.string.last_submit))
//        Log.d("delayTime", "passed in myDelayManager: $currentTimeMillis")
        if (!responseCounter.checkRandomComplete() && triggerIndex < triggerList.size) {
            if (
                (tempTag == "count_once_met") or
                MyPreferenceManager.getBoolean(
                    context,
                    context.getString(R.string.using_testing_triggers)
                ) or
                ((currentTimeMillis + triggerList[triggerIndex] * 1_000L) > (lastSubmit + 900_000L))
            ) {
                val triggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
                    .setInitialDelay(triggerList[triggerIndex].toLong(), TimeUnit.SECONDS)
                    .setInputData(
                        workDataOf(
                            "tag" to tempTag,
                            "unlock" to currentTimeMillis
                        )
                    )
                    .build()
                myWorkManager.enqueue(triggerRequest)
                randomRequestID = triggerRequest.id
//                Log.d(
//                    "unlock",
//                    "random trigger $triggerIndex is scheduled in ${triggerList[triggerIndex]} "
//                )
                triggerIndex++
            } else {
                Log.d(
                    "unlock",
                    "random trigger needs to wait for 15 minutes."
                )
            }
        } else {
            Log.d(
                "unlock",
                "random triggers completed."
            )
        }
        if (!responseCounter.check20Complete()) {
            val twentyTriggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
//                .setInitialDelay(extraTriggerTwenty, TimeUnit.MINUTES)
                .setInitialDelay(extraTriggerTwenty, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        "tag" to context.getString(R.string.count_20_met),
                        "unlock" to currentTimeMillis
                    )
                )
                .build()
            myWorkManager.enqueue(twentyTriggerRequest)
            twentyRequestID = twentyTriggerRequest.id
//            Log.d(
//                "unlock",
//                "a fixed trigger is scheduled in 20 " + if (MyPreferenceManager.getBoolean(
//                        context,
//                        context.getString(R.string.using_testing_triggers)
//                    )
//                ) "sec" else "min"
//            )
        } else if (!responseCounter.check30Complete()) {
            val thirtyTriggerRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
//                .setInitialDelay(extraTriggerThirty, TimeUnit.MINUTES)
                .setInitialDelay(extraTriggerThirty, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        "tag" to context.getString(R.string.count_30_met),
                        "unlock" to currentTimeMillis
                    )
                )
                .build()
            myWorkManager.enqueue(thirtyTriggerRequest)
            thirtyRequestID = thirtyTriggerRequest.id
//            Log.d(
//                "unlock",
//                "a fixed trigger is scheduled in 30 " + if (MyPreferenceManager.getBoolean(
//                        context,
//                        context.getString(R.string.using_testing_triggers)
//                    )
//                ) "sec" else "min"
//            )
        } else {
            Log.d("unlock", "fixed triggers completed.")
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
            if (myWorkManager.getWorkInfoById(randomRequestID).get()?.state
                != WorkInfo.State.SUCCEEDED
            ) {
                myWorkManager.cancelWorkById(randomRequestID)
//                Log.d("unlock", "a random trigger is canceled")
                recycleTrigger()
            }
        }
    }

    private fun checkAndCancelFixedTrigger(requestID: UUID) {
        if (myWorkManager.getWorkInfoById(requestID).get()?.state
            != WorkInfo.State.SUCCEEDED
        ) {
            myWorkManager.cancelWorkById(requestID)
//            Log.d("unlock", "a fixed trigger is canceled")
        }
    }

    fun recycleTrigger() {
        if (triggerIndex > 0) {
            triggerList += triggerList[triggerIndex - 1]
//            Log.d("unlock", "Trigger ${triggerList[triggerIndex - 1]} added to the list.")
        }
    }

    fun resetTriggerList() {
        triggerList = originalTriggerList.shuffled().toMutableList()
        triggerIndex = 0
//        Log.d("unlock", "trigger list is reset")
    }

}