package com.example.einkaufsliste.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val _isReady = MutableStateFlow(auth.currentUser != null)

    val isReady: StateFlow<Boolean> = _isReady

    init {
        signInInBackground()
    }

    suspend fun ensureSignedIn(): Boolean {
        if (auth.currentUser != null) {
            _isReady.value = true
            return true
        }

        return runCatching {
            auth.signInAnonymously().await()
            auth.currentUser != null
        }.getOrDefault(false).also { signedIn ->
            _isReady.value = signedIn
        }
    }

    fun markUnavailable() {
        _isReady.value = false
    }

    private fun signInInBackground() {
        if (auth.currentUser != null) {
            _isReady.value = true
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                _isReady.value = task.isSuccessful && auth.currentUser != null
            }
    }
}
