package com.smartmeet.app.ui.screens.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { authRepository.login(email, password) }
                .onSuccess {
                    _uiState.update { state -> state.copy(loading = false, isLoggedIn = true) }
                    onSuccess()
                }
                .onFailure {
                    _uiState.update { state -> state.copy(loading = false, error = it.message ?: "Login gagal") }
                }
        }
    }

    fun register(fullName: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { authRepository.register(fullName, email, password) }
                .onSuccess {
                    _uiState.update { state -> state.copy(loading = false) }
                    onSuccess()
                }
                .onFailure {
                    _uiState.update { state -> state.copy(loading = false, error = it.message ?: "Register gagal") }
                }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { authRepository.loginWithGoogle(idToken) }
                .onSuccess {
                    _uiState.update { state -> state.copy(loading = false, isLoggedIn = true) }
                    onSuccess()
                }
                .onFailure {
                    _uiState.update { state -> state.copy(loading = false, error = it.message ?: "Google login gagal") }
                }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}

data class AuthUiState(
    val loading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
