package com.jianzheng.studyzero.tool

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jianzheng.studyzero.R
import java.util.concurrent.TimeUnit

/**
 * Schedules the ESM triggers after each unlock.
 *
 * All state (shuffled delay list, index, pending random trigger) is persisted in
 * SharedPreferences so it survives process death. The list is only reshuffled by
 * [resetTriggerList] (daily reset / tutorial) or when the trigger mode changes.
 */
object MyDelayManager {
    const val ESM_WORK_TAG = "esm_trigger"
    const val KEY_IS_LIST_TRIGGER = "is_list_trigger"

    private const val RANDOM_COOL_DOWN_MINUTES = 60L

    private const val TAG_RANDOM = "count_random_met"
    private const val TAG_ONCE = "count_once_met"

    private const val PREF_LIST = "delay_trigger_list"
    private const val PREF_INDEX = "delay_trigger_index"
    private const val PREF_MODE = "delay_trigger_mode"
    private const val PREF_PENDING_ID = "delay_pending_random_id"
    private const val PREF_PENDING_DELAY = "delay_pending_random_delay"
    private const val PREF_LAST_FIRED_DELAY = "delay_last_fired_delay"

    private val longTriggerList = listOf(5, 8, 12, 18, 27, 41, 62, 95, 144, 220, 335, 510, 776)
    private val onceTriggerList = listOf(1)
    private const val EXTRA_TRIGGER_THIRTY = 30L  // minutes
    private const val EXTRA_TRIGGER_TWENTY = 20L  // minutes

    private val lock = Any()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

    private fun isOnceMode(context: Context): Boolean =
        MyPreferenceManager.getBoolean(context, context.getString(R.string.using_once_triggers))

    private fun modeTag(context: Context) = if (isOnceMode(context)) TAG_ONCE else TAG_RANDOM

    private fun readList(p: SharedPreferences): MutableList<Int> =
        p.getString(PREF_LIST, null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.toMutableList()
            ?: mutableListOf()

    /** Ensures a persisted list exists for the current mode. Never reshuffles otherwise. */
    private fun ensureState(context: Context) {
        val p = prefs(context)
        val mode = modeTag(context)
        if (!p.contains(PREF_LIST) || p.getString(PREF_MODE, null) != mode) {
            reshuffle(context, mode)
        }
    }

    private fun reshuffle(context: Context, mode: String = modeTag(context)) {
        val base = if (mode == TAG_ONCE) onceTriggerList else longTriggerList
        prefs(context).edit(commit = true) {
            putString(PREF_LIST, base.shuffled().joinToString(","))
            putInt(PREF_INDEX, 0)
            putString(PREF_MODE, mode)
            remove(PREF_PENDING_ID)
            remove(PREF_PENDING_DELAY)
            remove(PREF_LAST_FIRED_DELAY)
        }
    }

    fun init(context: Context) {
        synchronized(lock) { ensureState(context) }
        Log.d("unlock", "My delay manager initialized")
    }

    fun scheduleOverlayService(context: Context) {
        val appContext = context.applicationContext
        // Clean up anything left over in case a SCREEN_OFF broadcast was missed.
        cancelScheduledService(appContext)

        val workManager = WorkManager.getInstance(appContext)
        val responseCounter = ResponseCounter(appContext)
        if (responseCounter.checkDailyLimitReached()) {
            return
        }
        val currentTimeMillis = System.currentTimeMillis()
        val lastSubmit =
            MyPreferenceManager.getLong(appContext, appContext.getString(R.string.last_submit))

        synchronized(lock) {
            ensureState(appContext)
            val p = prefs(appContext)
            val mode = p.getString(PREF_MODE, TAG_RANDOM) ?: TAG_RANDOM
            val list = readList(p)
            val index = p.getInt(PREF_INDEX, 0)
            Log.d(
                "unlock",
                "TriggerIndex: $index, triggerListSize: ${list.size}, random completed: ${responseCounter.checkRandomComplete()}"
            )
            if (!responseCounter.checkRandomComplete() && index < list.size) {
                val delay = list[index]
                if (mode == TAG_ONCE ||
                    (currentTimeMillis + delay * 1_000L) >
                    (lastSubmit + TimeUnit.MINUTES.toMillis(RANDOM_COOL_DOWN_MINUTES))
                ) {
                    val request = OneTimeWorkRequestBuilder<MyTriggerWorker>()
                        .setInitialDelay(delay.toLong(), TimeUnit.SECONDS)
                        .addTag(ESM_WORK_TAG)
                        .setInputData(
                            workDataOf(
                                "tag" to mode,
                                "unlock" to currentTimeMillis,
                                KEY_IS_LIST_TRIGGER to true
                            )
                        )
                        .build()
                    p.edit(commit = true) {
                        putInt(PREF_INDEX, index + 1)
                        putString(PREF_PENDING_ID, request.id.toString())
                        putInt(PREF_PENDING_DELAY, delay)
                    }
                    workManager.enqueue(request)
                    Log.d("unlock", "random trigger $index is scheduled in $delay seconds")
                } else {
                    Log.d(
                        "unlock",
                        "random trigger needs to wait for $RANDOM_COOL_DOWN_MINUTES minutes."
                    )
                }
            } else {
                Log.d("unlock", "random triggers completed.")
            }
        }

        val fixedTag: String
        val fixedDelay: Long
        if (!responseCounter.check20Complete()) {
            fixedTag = appContext.getString(R.string.count_20_met)
            fixedDelay = EXTRA_TRIGGER_TWENTY
        } else if (!responseCounter.check30Complete()) {
            fixedTag = appContext.getString(R.string.count_30_met)
            fixedDelay = EXTRA_TRIGGER_THIRTY
        } else {
            Log.d("unlock", "fixed triggers completed.")
            return
        }
        val fixedRequest = OneTimeWorkRequestBuilder<MyTriggerWorker>()
            .setInitialDelay(fixedDelay, TimeUnit.MINUTES)
            .addTag(ESM_WORK_TAG)
            .setInputData(
                workDataOf(
                    "tag" to fixedTag,
                    "unlock" to currentTimeMillis,
                    KEY_IS_LIST_TRIGGER to false
                )
            )
            .build()
        workManager.enqueue(fixedRequest)
        Log.d("unlock", "a fixed trigger is scheduled in $fixedDelay minutes")
    }

    /**
     * Cancels all pending ESM triggers. The cancel is enqueued on WorkManager's serial
     * executor before any later enqueue, so it can never cancel a trigger scheduled afterwards.
     * Whether to recycle the random delay is decided from persisted state, which the worker
     * clears when it fires.
     */
    fun cancelScheduledService(context: Context) {
        val appContext = context.applicationContext
        WorkManager.getInstance(appContext).cancelAllWorkByTag(ESM_WORK_TAG)
        synchronized(lock) {
            val p = prefs(appContext)
            val pendingDelay = p.getInt(PREF_PENDING_DELAY, -1)
            if (pendingDelay >= 0) {
                val list = readList(p)
                list += pendingDelay
                p.edit(commit = true) {
                    putString(PREF_LIST, list.joinToString(","))
                    remove(PREF_PENDING_ID)
                    remove(PREF_PENDING_DELAY)
                }
                Log.d("unlock", "a random trigger is canceled, delay $pendingDelay recycled")
            }
        }
    }

    /** Called by [MyTriggerWorker] when a list (random/once) trigger fires. */
    fun onRandomTriggerFired(context: Context, workId: String) {
        synchronized(lock) {
            val p = prefs(context)
            if (p.getString(PREF_PENDING_ID, null) == workId) {
                val delay = p.getInt(PREF_PENDING_DELAY, -1)
                p.edit(commit = true) {
                    remove(PREF_PENDING_ID)
                    remove(PREF_PENDING_DELAY)
                    putInt(PREF_LAST_FIRED_DELAY, delay)
                }
            }
        }
    }

    /** Puts the delay of the last fired random trigger back at the end of the list. */
    fun recycleTrigger(context: Context) {
        synchronized(lock) {
            val p = prefs(context)
            val delay = p.getInt(PREF_LAST_FIRED_DELAY, -1)
            if (delay >= 0) {
                val list = readList(p)
                list += delay
                p.edit(commit = true) {
                    putString(PREF_LIST, list.joinToString(","))
                    remove(PREF_LAST_FIRED_DELAY)
                }
                Log.d("unlock", "Trigger $delay added to the list.")
            }
        }
    }

    fun resetTriggerList(context: Context) {
        synchronized(lock) { reshuffle(context) }
        Log.d("unlock", "trigger list is reset")
    }
}
