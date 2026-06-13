package com.smartmeet.app.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmeet.app.ui.components.AudioWaveform
import com.smartmeet.app.ui.screens.session.viewmodel.RecordingViewModel
import com.smartmeet.app.ui.theme.AccentOrange
import com.smartmeet.app.ui.theme.Divider
import com.smartmeet.app.ui.theme.RecordingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit,
    onStop: (String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transcripts = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var noteText by remember { mutableStateOf("") }
    var showStopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.startRecording() }

    LaunchedEffect(Unit) {
        viewModel.transcriptChunks.collect { chunk ->
            transcripts.add(chunk.text)
            if (transcripts.size > 0) listState.animateScrollToItem(transcripts.size - 1)
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Recording?") },
            text = { Text("Audio akan diupload dan diproses.") },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false; viewModel.stopRecording(onStop) }) { Text("Stop") }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("Batal") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                actions = {
                    IconButton(onClick = {
                        if (uiState.isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                    }) {
                        Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    }
                    IconButton(onClick = { showStopDialog = true }) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = RecordingRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape)
                        .background(if (uiState.isRecording) RecordingRed else MaterialTheme.colorScheme.outline)
                )
                Text(if (uiState.isRecording) "REC" else if (uiState.isPaused) "PAUSED" else "STOPPED",
                    style = MaterialTheme.typography.labelMedium)
            }
            AudioWaveform(
                activeColor = AccentOrange,
                inactiveColor = Divider,
                isAnimating = uiState.isRecording
            )
            Text(uiState.formattedTime, style = MaterialTheme.typography.headlineLarge)
            Text("Live Transcript", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (transcripts.isEmpty()) {
                    item { Text("Menunggu audio...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                }
                items(transcripts) { chunk ->
                    Text(chunk, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("📝 Tambah catatan...") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    if (noteText.isNotBlank()) { viewModel.addNote(noteText); noteText = "" }
                }) { Text("Kirim") }
            }
        }
    }
}
