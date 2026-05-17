package com.tushartamrakar.ontime.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.tushartamrakar.ontime.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ─── Auth state ───────────────────────────────────────────────────────────
    val currentUser: StateFlow<FirebaseUser?> = MutableStateFlow(
        authRepository.currentUser
    ).asStateFlow()

    // ─── UI state ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ─── Auth flow for navigation ─────────────────────────────────────────────
    val authState = authRepository.authState

    // ─── Login ────────────────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState(
                        error = getErrorMessage(exception.message),
                    )
                },
            )
        }
    }

    // ─── Register ─────────────────────────────────────────────────────────────
    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.register(name, email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState(
                        error = getErrorMessage(exception.message),
                    )
                },
            )
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }

    // ─── Clear error ──────────────────────────────────────────────────────────
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ─── Error messages ───────────────────────────────────────────────────────
    private fun getErrorMessage(message: String?): String {
        return when {
            message == null -> "Something went wrong. Please try again."
            message.contains("email address is already in use") ->
                "This email is already registered."
            message.contains("password is invalid") ||
                    message.contains("wrong password") ->
                "Incorrect password. Please try again."
            message.contains("no user record") ||
                    message.contains("user may have been deleted") ->
                "No account found with this email."
            message.contains("badly formatted") ->
                "Please enter a valid email address."
            message.contains("password should be at least") ->
                "Password must be at least 6 characters."
            message.contains("network error") ||
                    message.contains("unable to resolve host") ->
                "Network error. Please check your connection."
            message.contains("too many requests") ->
                "Too many attempts. Please try again later."
            else -> "Something went wrong. Please try again."
        }
    }
}