package com.smartmeet.app.ui.screens.session.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.api.models.Participant
import com.smartmeet.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewSessionUiState())
    val uiState: StateFlow<NewSessionUiState> = _uiState.asStateFlow()

    fun createSession(
        title: String,
        category: String,
        language: String,
        mode: String,
        participants: List<String>,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                sessionRepository.createSession(
                    title = title,
                    category = category,
                    language = language,
                    recordingMode = mode,
                    participants = participants.filter { it.isNotBlank() }.map { Participant(name = it) }
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(loading = false) }
                onSuccess(it.id)
            }.onFailure {
                _uiState.update { state -> state.copy(loading = false, error = it.message ?: "Gagal buat sesi") }
            }
        }
    }
}

data class NewSessionUiState(
    val loading: Boolean = false,
    val error: String? = null
)
