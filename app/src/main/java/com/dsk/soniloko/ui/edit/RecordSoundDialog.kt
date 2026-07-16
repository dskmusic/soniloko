package com.dsk.soniloko.ui.edit

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.dsk.soniloko.R
import com.dsk.soniloko.audio.AudioTrim
import com.dsk.soniloko.ui.components.TrimControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class RecordState { IDLE, RECORDING, RECORDED }

@Composable
fun RecordSoundDialog(onDismiss: () -> Unit, onSaved: (name: String, tempFile: File) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var state by remember { mutableStateOf(RecordState.IDLE) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var durationMs by remember { mutableIntStateOf(0) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    val tempFile = remember { File(context.cacheDir, "soniloko_recording_${System.currentTimeMillis()}.m4a") }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching { stop(); release() }
        }
    }

    LaunchedEffect(state) {
        if (state == RecordState.RECORDING) {
            elapsedSec = 0
            while (true) {
                delay(1000)
                elapsedSec++
            }
        }
    }

    fun startRecording() {
        val r = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder())
        runCatching {
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // Without these, MediaRecorder falls back to the device's AAC codec defaults,
                // which on many phones is as low as 8kHz/12kbps — audibly muffled/robotic voice.
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            recorder = r
            state = RecordState.RECORDING
        }
    }

    fun stopRecording() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        durationMs = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(tempFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
            } finally {
                retriever.release()
            }
        }.getOrDefault(0)
        trimRange = 0f..1f
        state = RecordState.RECORDED
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(300.dp)) {
                Text(stringResource(R.string.record_sound_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                if (!hasPermission) {
                    Text(stringResource(R.string.record_permission_needed), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                } else when (state) {
                    RecordState.IDLE -> {
                        Text(stringResource(R.string.tap_to_record), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { startRecording() }) {
                            Text(stringResource(R.string.record))
                        }
                    }
                    RecordState.RECORDING -> {
                        Text("${elapsedSec}s", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { stopRecording() }) {
                            Text(stringResource(R.string.stop_recording))
                        }
                    }
                    RecordState.RECORDED -> {
                        OutlinedButton(onClick = { state = RecordState.IDLE }) {
                            Text(stringResource(R.string.record_again))
                        }

                        if (durationMs > 0) {
                            Spacer(Modifier.height(16.dp))
                            TrimControls(tempFile, durationMs, trimRange) { trimRange = it }
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.sound_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = state == RecordState.RECORDED && name.isNotBlank() && !saving,
                        onClick = {
                            val isFullRange = trimRange.start <= 0.001f && trimRange.endInclusive >= 0.999f
                            if (isFullRange || durationMs <= 0) {
                                onSaved(name.trim(), tempFile)
                            } else {
                                saving = true
                                scope.launch {
                                    val startMs = (trimRange.start * durationMs).toLong()
                                    val endMs = (trimRange.endInclusive * durationMs).toLong()
                                    val trimmed = AudioTrim.trim(context, tempFile, startMs, endMs)
                                    saving = false
                                    onSaved(name.trim(), trimmed ?: tempFile)
                                }
                            }
                        }
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}
