package com.example.alarm_for_student

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

object AuthUtils {
    private var auth: FirebaseAuth? = null

    fun getAuthInstance(): FirebaseAuth {
        if (auth == null) {
            auth = FirebaseAuth.getInstance() // This will throw if not initialized
        }
        return auth!!
    }

    fun registerUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        getAuthInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        getAuthInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun logout() {
        auth?.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }
}

