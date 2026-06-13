package com.smartmeet.app.ui.screens.document.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.api.models.DocumentResponse
import com.smartmeet.app.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DocumentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository
) : ViewModel() {
    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    fun generate(formats: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(generating = true, error = null) }
            runCatching { documentRepository.generate(sessionId, formats) }
                .onSuccess { docs ->
                    _uiState.update { it.copy(generating = false, documents = docs) }
                    pollUntilDone()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(generating = false, error = err.message) }
                }
        }
    }

    private fun pollUntilDone() {
        viewModelScope.launch {
            repeat(20) {
                delay(3000)
                runCatching { documentRepository.getDocuments(sessionId) }
                    .onSuccess { docs ->
                        _uiState.update { it.copy(documents = docs) }
                        if (docs.all { it.status == "completed" || it.status == "failed" }) return@launch
                    }
            }
        }
    }
}

data class DocumentUiState(
    val generating: Boolean = false,
    val documents: List<DocumentResponse> = emptyList(),
    val error: String? = null
)
