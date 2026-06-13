package com.smartmeet.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmeet.app.ui.components.SessionCard
import com.smartmeet.app.ui.screens.dashboard.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewSession: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val processing = sessions.filter { it.status == "processing" }
    val recent = sessions.take(5)
    val totalSeconds = sessions.sumOf { it.audioDurationSeconds ?: 0 }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SmartMeet") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; onOpenLibrary() },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; onOpenSettings() },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewSession) {
                Text("⏺")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total sesi", style = MaterialTheme.typography.labelMedium)
                        Text(sessions.size.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                    Column {
                        Text("Jam rekaman", style = MaterialTheme.typography.labelMedium)
                        Text(String.format("%.2f", totalSeconds / 3600f), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            item { Text("Sesi Terbaru", style = MaterialTheme.typography.titleLarge) }
            if (uiState.loading) {
                item { CircularProgressIndicator() }
            }
            items(recent) { session ->
                SessionCard(session = session, onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth())
            }
            if (recent.isEmpty() && !uiState.loading) {
                item { Text("Belum ada sesi. Mulai rekam rapat pertamamu!", style = MaterialTheme.typography.bodyMedium) }
            }
            if (processing.isNotEmpty()) {
                item { Text("Sedang Diproses", style = MaterialTheme.typography.titleLarge) }
                items(processing) { session ->
                    SessionCard(session = session, onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (!uiState.error.isNullOrBlank()) {
                item { Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
