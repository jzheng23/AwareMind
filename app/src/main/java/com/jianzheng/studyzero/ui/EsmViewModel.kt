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
import com.jianzheng.studyzero.data.response.Response
import com.jianzheng.studyzero.data.response.ResponseDatabase
import com.jianzheng.studyzero.data.response.ResponseNoId
import com.jianzheng.studyzero.data.response.toDismissedEsm
import com.jianzheng.studyzero.data.response.toResponse
import com.jianzheng.studyzero.tool.MyPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class EsmViewModel(
    val context: Context
) : EsmViewModelInterface {
    private val db =
        Room.databaseBuilder(context, ResponseDatabase::class.java, "response_database")
            .fallbackToDestructiveMigration(false)
            .build()
    private val responseDao = db.responseDao()
    private val _uiState = MutableStateFlow(EsmUiState(0, false))
    override val uiState: StateFlow<EsmUiState> = _uiState.asStateFlow()


    private val database = Firebase.database
    private val myRef = database.getReference("ESM")
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

    override fun setAnswer(selection: Int) {
        //Log.d("viewModel", "$index is set to ${uiState.value.answer[index]}")
        _uiState.update { currentState ->
            currentState.copy(
                answer = selection,
                isAnswerValid = (selection != 0)
            )
        }
    }

    override suspend fun saveAnswer(delay: Long, startingTime: Long) {
        try {
            val newResponse = uiState.value.toResponse(delay, startingTime)
            responseDao.insert(newResponse)

            // Get the response with error handling
            val lastResponse = responseDao.getLastResponse()
            uploadToFirebase(lastResponse)


            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("EsmViewModel", "Error saving answer", e)
            // Still update preference even if upload fails
            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        }
    }

    private fun uploadToFirebase(newResponse: Response) {
        try {
            val uid = sharedPreferences.getString("user_ID", "")
            val host = HostManager.getHost(context)
            val todayDate = LocalDate.now().toString()
            if (validateUid(uid)) {
                myRef.child(uid.toString()).child(host).child(todayDate)
                    .child(newResponse.id.toString())
                    .setValue(ResponseNoId.fromResponse(newResponse))
                    .addOnFailureListener { exception ->
                        Log.e("EsmViewModel", "Firebase upload failed", exception)
                    }
            }
        } catch (e: Exception) {
            Log.e("EsmViewModel", "Error uploading to Firebase", e)
        }
    }


    override suspend fun saveDismissal(
        dismissalType: DismissalType,
        delay: Long,
        startingTime: Long
    ) {
        try {
            val dismissedResponse = Response(
                answer = 0, // No answer provided
                delay = delay,
                startingTime = startingTime,
                submittingTime = System.currentTimeMillis(),
                isSubmitted = false,
                dismissalType = dismissalType.name
            )
            responseDao.insert(dismissedResponse)

            val lastResponse = responseDao.getLastResponse()
            uploadDismissalToFirebase(lastResponse)

            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("EsmViewModel", "Error saving dismissal", e)
            // Still update preference even if database/upload fails
            MyPreferenceManager.setLong(
                context,
                context.getString(R.string.last_submit),
                System.currentTimeMillis()
            )
        }
    }

    private fun uploadDismissalToFirebase(dismissedResponse: Response) {
        try {
            val uid = sharedPreferences.getString("user_ID", "")
            val host = HostManager.getHost(context)
            val todayDate = LocalDate.now().toString()
            if (validateUid(uid)) {
                val dismissedEsm = dismissedResponse.toDismissedEsm()
                myRef.parent?.child("ESM_dismissed")
                    ?.child(uid.toString())
                    ?.child(host)
                    ?.child(todayDate)
                    ?.child(dismissedResponse.id.toString())
                    ?.setValue(dismissedEsm)
                    ?.addOnFailureListener { exception ->
                        Log.e("EsmViewModel", "Firebase dismissal upload failed", exception)
                    }
            }
        } catch (e: Exception) {
            Log.e("EsmViewModel", "Error uploading dismissal to Firebase", e)
        }
    }

}




class MockEsmViewModel : EsmViewModelInterface {
    override val uiState = MutableStateFlow(EsmUiState(0, false))
    override fun setAnswer(selection: Int) {
    }

    override suspend fun saveAnswer(delay: Long, startingTime: Long) {
        // Mock implementation
    }

    override suspend fun saveDismissal(
        dismissalType: DismissalType,
        delay: Long,
        startingTime: Long
    ) {
        // Mock implementation
    }
}