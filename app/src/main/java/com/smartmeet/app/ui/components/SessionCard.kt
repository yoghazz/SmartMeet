package com.smartmeet.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartmeet.app.data.db.entities.SessionEntity

@Composable
fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.category, style = MaterialTheme.typography.labelMedium)
                StatusChip(session.status)
            }
            Text(session.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${session.createdAt} • ${session.audioDurationSeconds ?: 0}s",
                style = MaterialTheme.typography.bodySmall
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("PDF", "DOCX", "PPTX")) { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
