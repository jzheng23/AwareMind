package com.jianzheng.studyzero.data

import com.google.firebase.database.FirebaseDatabase

object ClaimManager {
    private val database = FirebaseDatabase.getInstance()
    private val claimsRef = database.getReference("claims")

    fun uploadClaim(userId: String, response: ClaimResponse, onComplete: (Boolean) -> Unit) {
        claimsRef.child(userId).setValue(response)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
}
