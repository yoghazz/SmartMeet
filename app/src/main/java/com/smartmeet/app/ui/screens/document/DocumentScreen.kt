package com.smartmeet.app.ui.screens.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmeet.app.ui.components.LoadingButton
import com.smartmeet.app.ui.screens.document.viewmodel.DocumentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    onBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pdf by remember { mutableStateOf(true) }
    var docx by remember { mutableStateOf(false) }
    var pptx by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Generate Laporan") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Format Output", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = pdf, onCheckedChange = { pdf = it }); Text("PDF") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = docx, onCheckedChange = { docx = it }); Text("Word (.docx)") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = pptx, onCheckedChange = { pptx = it }); Text("PowerPoint (.pptx)") }
            if (!uiState.error.isNullOrBlank()) Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            LoadingButton(
                text = if (uiState.generating) "Generating..." else "Generate",
                loading = uiState.generating,
                onClick = {
                    val formats = buildList { if (pdf) add("pdf"); if (docx) add("docx"); if (pptx) add("pptx") }
                    if (formats.isNotEmpty()) viewModel.generate(formats)
                }
            )
            if (uiState.generating) CircularProgressIndicator()
            uiState.documents.forEach { doc ->
                ListItem(
                    headlineContent = { Text(doc.format.uppercase()) },
                    supportingContent = { Text(doc.status) },
                    trailingContent = { if (!doc.downloadUrl.isNullOrBlank()) Text("Download", color = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}
