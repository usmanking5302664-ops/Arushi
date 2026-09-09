package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AssistantState

@Composable
fun PulsingOrb(
    state: AssistantState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 700
                    AssistantState.SPEAKING -> 500
                    AssistantState.THINKING, AssistantState.EXECUTING_ACTION -> 400
                    AssistantState.IDLE -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING, AssistantState.EXECUTING_ACTION -> 2500
                    AssistantState.SPEAKING -> 4000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = when (state) {
        AssistantState.LISTENING -> Color(0xFF10B981) // Emerald Green
        AssistantState.SPEAKING -> Color(0xFF6366F1) // Vibrant Indigo
        AssistantState.THINKING -> Color(0xFFF59E0B) // Amber Gold
        AssistantState.EXECUTING_ACTION -> Color(0xFF06B6D4) // Electric Cyan
        AssistantState.IDLE -> Color(0xFF818CF8) // Soft Indigo
    }

    val secondaryColor = when (state) {
        AssistantState.LISTENING -> Color(0xFF34D399)
        AssistantState.SPEAKING -> Color(0xFFA855F7)
        AssistantState.THINKING -> Color(0xFFF97316)
        AssistantState.EXECUTING_ACTION -> Color(0xFF3B82F6)
        AssistantState.IDLE -> Color(0xFFC084FC)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(160.dp)
            .testTag("pulsing_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2.6f * pulseScale

            // Outer ethereal ripple
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = baseRadius * 1.28f,
                center = center
            )

            // Mid glow ring
            drawCircle(
                color = secondaryColor.copy(alpha = 0.25f),
                radius = baseRadius * 1.12f,
                center = center
            )

            // Inner gradient orb core
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor,
                    primaryColor,
                    primaryColor.copy(alpha = 0.8f)
                ),
                center = center,
                radius = baseRadius
            )

            drawCircle(
                brush = gradientBrush,
                radius = baseRadius,
                center = center
            )

            // Core highlight reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = baseRadius * 0.35f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )
        }
    }
}
