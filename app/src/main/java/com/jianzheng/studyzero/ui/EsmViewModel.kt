package com.jianzheng.studyzero.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.jianzheng.studyzero.data.response.EsmUiState
import com.jianzheng.studyzero.data.response.Response
import com.jianzheng.studyzero.data.response.ResponseDao
import com.jianzheng.studyzero.data.response.ResponseNoId
import com.jianzheng.studyzero.data.response.toResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EsmViewModel(
    private val responseDao: ResponseDao,
    val context: Context
) {
    private val _uiState = MutableStateFlow(EsmUiState(listOf(0, 0), false))
    val uiState: StateFlow<EsmUiState> = _uiState.asStateFlow()
    private val database = Firebase.database
    private val myRef = database.getReference("ESM")
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

    fun setAnswer(selection: Int, index: Int) {
        val updatedAnswers = uiState.value.answer.toMutableList()
        updatedAnswers[index] = selection
        //Log.d("viewModel", "$index is set to ${uiState.value.answer[index]}")
        _uiState.update { currentState ->
            currentState.copy(
                answer = updatedAnswers,
                isAnswerValid = (updatedAnswers[0] * updatedAnswers[1] != 0)
            )
        }
    }

    suspend fun saveAnswer(delay: Long, startingTime: Long) {
        val newResponse = uiState.value.toResponse(delay, startingTime)
        responseDao.insert(newResponse)
        uploadToFirebase(responseDao.getLastResponse())
        Log.d("data", "${responseDao.getLastResponse().startingTime}")
        Handler(Looper.getMainLooper()).post {
            val toast = Toast.makeText(context, "Submitted successfully!", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun uploadToFirebase(newResponse: Response) {
        if (sharedPreferences.getBoolean("UID_valid", false)) {
            val uid = sharedPreferences.getString("UID", "null")
            myRef.child(uid.toString()).child(newResponse.id.toString())
                .setValue(ResponseNoId.fromResponse(newResponse))
        } else Log.d("userId", "invalid user id")
    }


}