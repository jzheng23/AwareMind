package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.HostManager
import com.jianzheng.studyzero.data.response.DismissalType
import com.jianzheng.studyzero.data.response.EsmUiState
import com.jianzheng.studyzero.data.response.ResponseStress
import com.jianzheng.studyzero.data.response.ResponseStressDatabase
import com.jianzheng.studyzero.data.response.ResponseStressNoId
import com.jianzheng.studyzero.data.response.toDismissedEsm
import com.jianzheng.studyzero.data.response.toResponseStress
import com.jianzheng.studyzero.tool.MyPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class EsmStressViewModel(
    val context: Context
) : EsmViewModelInterface {
    private val db =
        Room.databaseBuilder(context, ResponseStressDatabase::class.java, "response_stress_database")
            .fallbackToDestructiveMigration(false)
            .build()
    private val responseStressDao = db.responseStressDao()
    private val _uiState = MutableStateFlow(EsmUiState(0, false))
    override val uiState: StateFlow<EsmUiState> = _uiState.asStateFlow()

    private val database = Firebase.database
    private val myRef = database.getReference("ESM_stress")
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

    override fun setAnswer(selection: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                answer = selection,
                isAnswerValid = (selection != 0)
            )
        }
    }

    override suspend fun saveAnswer(delay: Long, startingTime: Long) {
        try {
            val newResponse = uiState.value.toResponseStress(delay, startingTime)
            responseStressDao.insert(newResponse)

            val lastResponse = responseStressDao.getLastResponse()
            uploadToFirebase(lastResponse)

            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("EsmStressViewModel", "Error saving answer", e)
            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        }
    }

    private fun uploadToFirebase(newResponse: ResponseStress) {
        try {
            val uid = sharedPreferences.getString("user_ID", "")
            val host = HostManager.getHost(context)
            val todayDate = LocalDate.now().toString()
            if (validateUid(uid)) {
                myRef.child(uid.toString()).child(host).child(todayDate)
                    .child(newResponse.id.toString())
                    .setValue(ResponseStressNoId.fromResponseStress(newResponse))
                    .addOnFailureListener { exception ->
                        Log.e("EsmStressViewModel", "Firebase upload failed", exception)
                    }
            }
        } catch (e: Exception) {
            Log.e("EsmStressViewModel", "Error uploading to Firebase", e)
        }
    }

    override suspend fun saveDismissal(
        dismissalType: DismissalType,
        delay: Long,
        startingTime: Long
    ) {
        try {
            val dismissedResponse = ResponseStress(
                answer = 0,
                delay = delay,
                startingTime = startingTime,
                submittingTime = System.currentTimeMillis(),
                isSubmitted = false,
                dismissalType = dismissalType.name
            )
            responseStressDao.insert(dismissedResponse)

            val lastResponse = responseStressDao.getLastResponse()
            uploadDismissalToFirebase(lastResponse)

            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("EsmStressViewModel", "Error saving dismissal", e)
            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        }
    }

    private fun uploadDismissalToFirebase(dismissedResponse: ResponseStress) {
        try {
            val uid = sharedPreferences.getString("user_ID", "")
            val host = HostManager.getHost(context)
            val todayDate = LocalDate.now().toString()
            if (validateUid(uid)) {
                val dismissedEsm = dismissedResponse.toDismissedEsm()
                myRef.parent?.child("ESM_stress_dismissed")
                    ?.child(uid.toString())
                    ?.child(host)
                    ?.child(todayDate)
                    ?.child(dismissedResponse.id.toString())
                    ?.setValue(dismissedEsm)
                    ?.addOnFailureListener { exception ->
                        Log.e("EsmStressViewModel", "Firebase dismissal upload failed", exception)
                    }
            }
        } catch (e: Exception) {
            Log.e("EsmStressViewModel", "Error uploading dismissal to Firebase", e)
        }
    }
}

interface EsmViewModelInterface {
    val uiState: StateFlow<EsmUiState>
    fun setAnswer(selection: Int)
    suspend fun saveAnswer(delay: Long, startingTime: Long)
    suspend fun saveDismissal(
        dismissalType: DismissalType,
        delay: Long,
        startingTime: Long
    )
}

class MockEsmStressViewModel : EsmViewModelInterface {
    override val uiState = MutableStateFlow(EsmUiState(0, false))
    override fun setAnswer(selection: Int) {}
    override suspend fun saveAnswer(delay: Long, startingTime: Long) {}
    override suspend fun saveDismissal(
        dismissalType: DismissalType,
        delay: Long,
        startingTime: Long
    ) {}
}
