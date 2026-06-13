package com.smartmeet.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}
