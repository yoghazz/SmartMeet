package com.smartmeet.app.ui.screens.session.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeet.app.data.api.WebSocketManager
import com.smartmeet.app.data.repository.AuthRepository
import com.smartmeet.app.data.repository.SessionRepository
import com.smartmeet.app.service.RecordingForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File

@HiltViewModel
class RecordingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager,
    private val retrofit: Retrofit
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    val transcriptChunks = webSocketManager.transcriptFlow
    val wsStatus = webSocketManager.statusFlow

    private var timerJob: Job? = null

    fun startRecording(mode: String = "realtime") {
        viewModelScope.launch {
            runCatching { sessionRepository.startRecording(sessionId) }
            val token = authRepository.getAccessToken() ?: return@launch
            if (mode == "realtime" || mode == "hybrid") {
                webSocketManager.connect(sessionId, token)
            }
            _uiState.update { it.copy(isRecording = true, mode = mode) }
            startTimer()
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                putExtra(RecordingForegroundService.EXTRA_SESSION_ID, sessionId)
                putExtra(RecordingForegroundService.EXTRA_MODE, mode)
            }
            context.startForegroundService(intent)
        }
    }

    fun pauseRecording() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRecording = false, isPaused = true) }
    }

    fun resumeRecording() {
        _uiState.update { it.copy(isRecording = true, isPaused = false) }
        startTimer()
    }

    fun stopRecording(onDone: (String) -> Unit) {
        timerJob?.cancel()
        webSocketManager.sendStop()
        webSocketManager.disconnect()
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = false, isPaused = false, uploading = true) }
            val mode = _uiState.value.mode
            if (mode == "batch" || mode == "hybrid") {
                uploadBatchAudio()
            }
            runCatching { sessionRepository.stopRecording(sessionId) }
            val stopIntent = Intent(context, RecordingForegroundService::class.java).apply {
                action = RecordingForegroundService.ACTION_STOP
            }
            context.startService(stopIntent)
            _uiState.update { it.copy(uploading = false) }
            onDone(sessionId)
        }
    }

    private suspend fun uploadBatchAudio() {
        val pcmFile = File(context.cacheDir, "smartmeet_$sessionId.pcm")
        if (!pcmFile.exists()) return
        runCatching {
            val requestBody = pcmFile.asRequestBody("audio/pcm".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", pcmFile.name, requestBody)
            sessionRepository.uploadAudio(sessionId, part)
        }
        pcmFile.delete()
    }

    fun addNote(note: String) {
        _uiState.update { it.copy(notes = it.notes + note) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val uploading: Boolean = false,
    val elapsedSeconds: Int = 0,
    val notes: List<String> = emptyList(),
    val mode: String = "realtime"
) {
    val formattedTime: String get() {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
