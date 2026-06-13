package com.smartmeet.app.ui.screens.library.viewmodel

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val filter: StateFlow<String> = _filter.asStateFlow()

    val sessions: StateFlow<List<SessionEntity>> = sessionRepository.observeCachedSessions()
        .catch { emit(emptyList()) }
        .combine(_query) { list, q ->
            if (q.isBlank()) list else list.filter { it.title.contains(q, ignoreCase = true) }
        }
        .combine(_filter) { list, f ->
            if (f == "all") list else list.filter { it.status == f }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.update { q } }
    fun setFilter(f: String) { _filter.update { f } }
    fun refresh() { viewModelScope.launch { runCatching { sessionRepository.refreshSessions() } } }
}
