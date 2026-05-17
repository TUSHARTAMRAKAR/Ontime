package com.tushartamrakar.ontime.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {
    // ─── Current user ─────────────────────────────────────────────────────────
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    // ─── Auth state as Flow ───────────────────────────────────────────────────
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Register ─────────────────────────────────────────────────────────────
    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            // Update display name
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            result.user!!.updateProfile(profileUpdates).await()

            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    fun logout() {
        firebaseAuth.signOut()
    }

    // ─── Is logged in ─────────────────────────────────────────────────────────
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}