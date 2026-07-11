package com.dsk.soniloko.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsk.soniloko.data.FontAwesomeIcons
import com.dsk.soniloko.data.ImageLibrary
import com.dsk.soniloko.data.model.SoundButtonConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundButtonItem(
    config: SoundButtonConfig,
    editMode: Boolean,
    faFontFamily: FontFamily?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val icon = remember(config.iconName) { FontAwesomeIcons.byName(config.iconName) }
    val customBitmap = remember(config.customImageFile) {
        config.customImageFile
            ?.let { ImageLibrary.resolveImageFile(it) }
            ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(22.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Relying only on the raw press/release interaction stream is unreliable for quick taps —
    // both events can land within the same frame, so `isPressed` may never even be observed as
    // true. Instead, trigger a guaranteed pulse directly from the confirmed tap callback itself
    // (fires unambiguously, regardless of gesture-detection timing), and combine it with the
    // natural held-down state so a long press still stays visibly pressed for its whole duration.
    val scope = rememberCoroutineScope()
    var tapPulse by remember { mutableStateOf(false) }
    val pressedVisual = isPressed || tapPulse

    val handleTap: () -> Unit = {
        onTap()
        scope.launch {
            tapPulse = true
            delay(150)
            tapPulse = false
        }
    }

    // While held past the actual long-press threshold, sink deeper (80% vs the normal tap's 90%)
    // so the user feels a distinct "extra" stage right as the long-press action is about to fire.
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    var isDeepPress by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(longPressTimeoutMillis)
            if (isPressed) isDeepPress = true
        } else {
            isDeepPress = false
        }
    }

    // A physical button sinks in and its shadow shortens while held, then springs back on release.
    val targetScale = if (isDeepPress) 0.80f else if (pressedVisual) 0.90f else 1f
    val pressScale by animateFloatAsState(targetScale, animationSpec = tween(60), label = "press-scale")
    val pressElevation by animateFloatAsState(
        if (pressedVisual) 1f else if (highlighted) 16f else 6f,
        animationSpec = tween(60),
        label = "press-elevation"
    )

    // Same vivid theme color as icon-only buttons: a custom image with real transparency
    // should show through to that, not a separate muted/near-black "surface" backdrop.
    val baseColor = MaterialTheme.colorScheme.primary
    val pressedColor = lerp(baseColor, Color.Black, 0.3f)
    val animatedColor by animateColorAsState(
        if (pressedVisual) pressedColor else baseColor,
        animationSpec = tween(60),
        label = "press-color"
    )

    Box(
        modifier
            .aspectRatio(1f)
            .scale(pressScale)
            .shadow(elevation = pressElevation.dp, shape = shape, clip = false)
            .clip(shape)
            .background(animatedColor)
            .then(
                if (customBitmap != null) Modifier.border(3.dp, MaterialTheme.colorScheme.onPrimary, shape)
                else Modifier
            )
            .then(
                if (editMode) Modifier.dashedBorder(MaterialTheme.colorScheme.onPrimary, 22.dp)
                else Modifier
            )
            .then(
                if (highlighted) Modifier.border(4.dp, Color.White, shape)
                else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = handleTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!config.customText.isNullOrBlank()) {
            AutoSizeText(
                text = config.customText,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
        } else {
            Text(
                text = icon?.codepoint ?: "?",
                fontFamily = faFontFamily,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        if (editMode) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
        if (highlighted) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)))
        }
    }
}
