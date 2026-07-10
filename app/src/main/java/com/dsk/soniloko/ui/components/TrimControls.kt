package com.dsk.soniloko.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dsk.soniloko.R
import com.dsk.soniloko.audio.AudioWaveform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shared trim UI, used by the recording, YouTube-download and edit-trim flows: waveform,
 * start/end range slider, and a play/pause button that previews only the selected range
 * (auto-stops at the end of the selection). Self-contained — releases its own player on
 * dispose, so callers don't need to manage playback state themselves.
 */
@Composable
fun TrimControls(
    file: File,
    durationMs: Int,
    trimRange: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    var waveform by remember(file.absolutePath) { mutableStateOf<FloatArray?>(null) }
    LaunchedEffect(file.absolutePath, durationMs) {
        waveform = withContext(Dispatchers.IO) { AudioWaveform.extract(file, durationMs) }
    }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var stopJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    var playheadFraction by remember { mutableStateOf<Float?>(null) }

    fun stopPreview() {
        stopJob?.cancel()
        stopJob = null
        player?.runCatching { stop(); release() }
        player = null
        isPlaying = false
        playheadFraction = null
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    // Polls the active player's position while it's playing, so the waveform can show a
    // moving playhead. Cancelled automatically by Compose whenever [player] changes/clears.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            val posMs = runCatching { p.currentPosition }.getOrNull() ?: break
            playheadFraction = (posMs.toFloat() / durationMs).coerceIn(0f, 1f)
            delay(30)
        }
    }

    fun playSelection() {
        stopPreview()
        val startMs = (trimRange.start * durationMs).toLong()
        val endMs = (trimRange.endInclusive * durationMs).toLong()
        runCatching {
            val p = MediaPlayer()
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener { stopPreview() }
            p.prepare()
            p.seekTo(startMs.toInt())
            p.start()
            player = p
            isPlaying = true
            stopJob = scope.launch {
                delay((endMs - startMs).coerceAtLeast(0))
                stopPreview()
            }
        }
    }

    Column {
        Text(stringResource(R.string.trim_hint), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Waveform(waveform, trimRange, playheadFraction, Modifier.fillMaxWidth().height(48.dp))
        if (waveform == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        RangeSlider(value = trimRange, onValueChange = { stopPreview(); onRangeChange(it) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${stringResource(R.string.trim_start)}: ${"%.1f".format(trimRange.start * durationMs / 1000f)}s",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                "${stringResource(R.string.trim_end)}: ${"%.1f".format(trimRange.endInclusive * durationMs / 1000f)}s",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { if (isPlaying) stopPreview() else playSelection() }) {
            Text(if (isPlaying) stringResource(R.string.stop_recording) else stringResource(R.string.play_recording))
        }
    }
}

@Composable
private fun Waveform(
    data: FloatArray?,
    trimRange: ClosedFloatingPointRange<Float>,
    playheadFraction: Float?,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val playheadColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val bars = data ?: return@Canvas
        if (bars.isEmpty()) return@Canvas
        val barWidth = size.width / bars.size
        val midY = size.height / 2f
        for (i in bars.indices) {
            val amp = bars[i].coerceIn(0f, 1f)
            val barHeight = (amp * size.height).coerceAtLeast(2f)
            val fraction = i.toFloat() / bars.size
            val inRange = fraction >= trimRange.start && fraction <= trimRange.endInclusive
            drawRect(
                color = if (inRange) activeColor else dimColor,
                topLeft = Offset(i * barWidth, midY - barHeight / 2f),
                size = Size((barWidth * 0.7f).coerceAtLeast(1f), barHeight)
            )
        }
        if (playheadFraction != null) {
            val x = playheadFraction.coerceIn(0f, 1f) * size.width
            drawLine(
                color = playheadColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
