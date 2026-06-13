package com.smartmeet.app.ui.screens.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.api.models.DocumentResponse
import com.smartmeet.app.data.api.models.SessionResponse
import com.smartmeet.app.data.repository.DocumentRepository
import com.smartmeet.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {
    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { sessionRepository.getSession(sessionId) }
                .onSuccess { session ->
                    val docs = runCatching { documentRepository.getDocuments(sessionId) }.getOrElse { emptyList() }
                    _uiState.update { it.copy(loading = false, session = session, documents = docs) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(loading = false, error = err.message) }
                }
        }
    }
}

data class SessionDetailUiState(
    val loading: Boolean = false,
    val session: SessionResponse? = null,
    val documents: List<DocumentResponse> = emptyList(),
    val error: String? = null
)
