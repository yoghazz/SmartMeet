package com.smartmeet.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    activeColor: Color,
    inactiveColor: Color,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val barWidth = size.width / (barCount * 1.8f)
        val gap = barWidth * 0.8f
        val totalWidth = barCount * (barWidth + gap)
        val startX = (size.width - totalWidth) / 2f

        repeat(barCount) { index ->
            val fraction = index.toFloat() / barCount
            val raw = if (isAnimating) {
                abs(sin(fraction * 3 * Math.PI.toFloat() + phase))
            } else {
                abs(sin(fraction * 3 * Math.PI.toFloat()).toFloat()) * 0.3f
            }
            val barHeight = size.height * (0.15f + raw * 0.85f)
            val left = startX + index * (barWidth + gap)
            val top = (size.height - barHeight) / 2f
            val color = if (raw > 0.5f) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
