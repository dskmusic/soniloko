package com.dsk.soniloko.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Grille of speaker holes with a subtle idle "breathing" animation, plus a short reaction
 * pulse (brighter, slightly larger dots) each time [pulseTrigger] changes — i.e. on every play.
 * When [gameLevel] is non-null (game mode active), a retro LED-style level/lives readout is
 * overlaid on top of the dots. When [kitNameFlashTrigger] changes, [kitNameFlash] briefly
 * fades in and out on top of the grille (e.g. after switching kits with a swipe).
 */
@Composable
fun SpeakerGrille(
    modifier: Modifier = Modifier,
    dotColor: Color,
    pulseTrigger: Int = 0,
    gameLevel: Int? = null,
    gameLives: Int = 0,
    kitNameFlash: String? = null,
    kitNameFlashTrigger: Int = 0
) {
    val idle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        idle.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
    }

    val kitNameAlpha = remember { Animatable(0f) }
    LaunchedEffect(kitNameFlashTrigger) {
        if (kitNameFlashTrigger > 0) {
            kitNameAlpha.snapTo(0f)
            kitNameAlpha.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            delay(650)
            kitNameAlpha.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier.fillMaxWidth().height(96.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dotColor.copy(alpha = 0.12f))
                .padding(10.dp)
        ) {
            val cols = 14
            val rows = 6
            val cellW = size.width / cols
            val cellH = size.height / rows
            val baseRadius = minOf(cellW, cellH) * 0.28f
            val idleAlpha = 0.55f + idle.value * 0.3f
            val dotAlpha = (idleAlpha + pulse.value * 0.2f).coerceIn(0f, 1f)
            val dotRadius = baseRadius * (1f + pulse.value * 0.35f)
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    drawCircle(
                        color = dotColor.copy(alpha = dotAlpha),
                        radius = dotRadius,
                        center = Offset(cellW * (col + 0.5f), cellH * (row + 0.5f))
                    )
                }
            }
        }

        if (gameLevel != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LV.$gameLevel   " + "♥".repeat(gameLives.coerceAtLeast(0)),
                    color = Color(0xFF39FF14),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 3.sp
                )
            }
        }

        if (kitNameFlash != null && kitNameAlpha.value > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f * kitNameAlpha.value)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = kitNameFlash,
                    color = Color.White.copy(alpha = kitNameAlpha.value),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
