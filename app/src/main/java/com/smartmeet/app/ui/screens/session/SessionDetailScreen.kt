package com.smartmeet.app.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmeet.app.ui.screens.session.viewmodel.SessionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onOpenDocuments: (String) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ringkasan", "Transkripsi", "Dokumen")

    Scaffold(topBar = {
        TopAppBar(title = { Text(uiState.session?.title ?: "Detail Sesi") })
    }) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) }) }
            }
            when (tab) {
                0 -> {
                    val session = uiState.session
                    if (session == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada data") }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!session.summary.isNullOrBlank()) {
                                item { Text("Rangkuman", style = MaterialTheme.typography.titleMedium) }
                                item { Text(session.summary) }
                                item { HorizontalDivider() }
                            }
                            if (!session.keyPoints.isNullOrEmpty()) {
                                item { Text("Poin-Poin Kunci", style = MaterialTheme.typography.titleMedium) }
                                items(session.keyPoints) { point -> Text("• $point") }
                                item { HorizontalDivider() }
                            }
                            if (!session.actionItems.isNullOrEmpty()) {
                                item { Text("Action Items", style = MaterialTheme.typography.titleMedium) }
                                items(session.actionItems) { item ->
                                    Text("☐ ${item.task}${item.assignee?.let { " — $it" } ?: ""}${item.dueDate?.let { " ($it)" } ?: ""}")
                                }
                                item { HorizontalDivider() }
                            }
                            if (!session.conclusions.isNullOrBlank()) {
                                item { Text("Kesimpulan", style = MaterialTheme.typography.titleMedium) }
                                item { Text(session.conclusions) }
                            }
                        }
                    }
                }
                1 -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(uiState.session?.manualNotes ?: "Tidak ada transkripsi")
                }
                2 -> Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.documents.isEmpty()) {
                        Button(onClick = { onOpenDocuments(viewModel.sessionId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Generate Laporan")
                        }
                    } else {
                        uiState.documents.forEach { doc ->
                            ListItem(
                                headlineContent = { Text(doc.format.uppercase()) },
                                supportingContent = { Text(doc.status) },
                                trailingContent = {
                                    if (!doc.downloadUrl.isNullOrBlank()) Text("Download", color = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
