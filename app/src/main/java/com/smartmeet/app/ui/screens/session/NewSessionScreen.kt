package com.smartmeet.app.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmeet.app.ui.components.LoadingButton
import com.smartmeet.app.ui.screens.session.viewmodel.NewSessionViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NewSessionScreen(
    onBack: () -> Unit,
    onSessionCreated: (String) -> Unit,
    viewModel: NewSessionViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("meeting") }
    var language by remember { mutableStateOf("id") }
    var mode by remember { mutableStateOf("realtime") }
    var participant by remember { mutableStateOf("") }
    val participants = remember { mutableStateListOf<String>() }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Sesi Baru") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nama Rapat") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Bahasa") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mode, onValueChange = { mode = it }, label = { Text("Mode") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = participant, onValueChange = { participant = it }, label = { Text("Peserta") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (participant.isNotBlank()) participants.add(participant)
                    participant = ""
                }) { Text("Add") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { name ->
                    AssistChip(onClick = { participants.remove(name) }, label = { Text(name) })
                }
            }
            if (!uiState.error.isNullOrBlank()) {
                Text(uiState.error.orEmpty())
            }
            LoadingButton(
                text = "Mulai Rekam",
                loading = uiState.loading,
                onClick = {
                    viewModel.createSession(title, category, language, mode, participants, onSessionCreated)
                }
            )
        }
    }
}
