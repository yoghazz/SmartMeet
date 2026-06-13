package com.smartmeet.app.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun StatusChip(status: String) {
    AssistChip(
        onClick = {},
        label = { Text(status.replaceFirstChar { it.uppercase() }) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}
