package com.smartmeet.app.ui.screens.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.db.entities.SessionEntity
import com.smartmeet.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    val sessions: StateFlow<List<SessionEntity>> = sessionRepository.observeCachedSessions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { sessionRepository.refreshSessions() }
                .onSuccess { _uiState.update { state -> state.copy(loading = false) } }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(loading = false, error = error.message ?: "Gagal muat sesi") }
                }
        }
    }
}

data class DashboardUiState(
    val loading: Boolean = false,
    val error: String? = null
)
