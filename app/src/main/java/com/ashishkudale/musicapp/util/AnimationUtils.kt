package com.ashishkudale.musicapp.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Crossfade animation for screen transitions
 */
@Composable
fun <T> CrossfadeTransition(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = tween(durationMillis = 300)
    ) { state ->
        content(state)
    }
}

/**
 * Slide in from bottom animation
 */
fun slideInVerticallySpec() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

fun slideOutVerticallySpec() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

/**
 * Fade in/out animations
 */
fun fadeInSpec() = fadeIn(animationSpec = tween(300))
fun fadeOutSpec() = fadeOut(animationSpec = tween(300))

/**
 * Scale animation modifier
 */
@Composable
fun Modifier.animateScale(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Pulsing animation for loading indicators
 */
@Composable
fun rememberPulseAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    ).value
}
