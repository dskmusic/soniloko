package com.dsk.soniloko.ui.edit

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.audio.AudioTrim
import com.dsk.soniloko.data.SettingsRepository
import com.dsk.soniloko.data.youtube.ReclipRepository
import com.dsk.soniloko.data.youtube.YoutubeRepository
import com.dsk.soniloko.data.youtube.YtSearchResult
import com.dsk.soniloko.ui.components.TrimControls
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private enum class YtStage { SEARCH, READY }
private enum class SortOrder { DURATION_ASC, DURATION_DESC, RELEVANCE }
private const val PAGE_SIZE = 15

/**
 * Search + preview + download an online sound source (client-side extraction via
 * NewPipeExtractor, no API key, no server — same technique as DSK LoFi). The downloaded
 * audio then goes through the same trim step as a fresh recording before it's named and saved.
 */
@Composable
fun YoutubeSearchDialog(onDismiss: () -> Unit, onSaved: (name: String, file: File) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<YtSearchResult>>(emptyList()) }
    var sortOrder by remember { mutableStateOf(SortOrder.DURATION_ASC) }
    var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }
    var searching by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var previewingId by remember { mutableStateOf<String?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }

    var stage by remember { mutableStateOf(YtStage.SEARCH) }
    var durationMs by remember { mutableIntStateOf(0) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var name by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val sortedResults = remember(results, sortOrder) {
        when (sortOrder) {
            SortOrder.DURATION_ASC -> results.sortedBy { it.durationSec }
            SortOrder.DURATION_DESC -> results.sortedByDescending { it.durationSec }
            SortOrder.RELEVANCE -> results
        }
    }
    val visibleResults = remember(sortedResults, visibleCount) { sortedResults.take(visibleCount) }

    fun stopPreview() {
        previewPlayer?.runCatching { stop(); release() }
        previewPlayer = null
        previewingId = null
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    fun doSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        stopPreview()
        searching = true
        errorMsg = null
        scope.launch {
            val r = YoutubeRepository.search(q)
            searching = false
            results = r
            visibleCount = PAGE_SIZE
            if (r.isEmpty()) errorMsg = context.getString(R.string.youtube_no_results)
        }
    }

    fun togglePreview(result: YtSearchResult) {
        if (previewingId == result.videoId) {
            stopPreview()
            return
        }
        stopPreview()
        previewingId = result.videoId
        scope.launch {
            val url = YoutubeRepository.resolvePreviewUrl(result.videoId)
            if (url == null) {
                previewingId = null
                errorMsg = context.getString(R.string.youtube_error)
                return@launch
            }
            runCatching {
                val p = MediaPlayer()
                p.setDataSource(url)
                p.setOnCompletionListener { previewingId = null }
                p.setOnPreparedListener { it.start() }
                p.prepareAsync()
                previewPlayer = p
            }.onFailure {
                previewingId = null
                errorMsg = context.getString(R.string.youtube_error)
            }
        }
    }

    fun startDownload(result: YtSearchResult) {
        stopPreview()
        downloadingId = result.videoId
        errorMsg = null
        scope.launch {
            val settings = runCatching { SettingsRepository(context).settings.first() }.getOrNull()
            var finalFile: File? = null

            if (settings != null && settings.reclipServerUrl.isNotBlank()) {
                val reclipTmp = File(context.cacheDir, "soniloko_yt_${System.currentTimeMillis()}_reclip.mp3")
                val ok = ReclipRepository.downloadAudio(
                    serverUrl = settings.reclipServerUrl,
                    username = settings.reclipUsername,
                    password = settings.reclipPassword,
                    videoId = result.videoId,
                    title = result.title,
                    destFile = reclipTmp
                )
                if (ok) finalFile = reclipTmp else reclipTmp.delete()
            }

            if (finalFile == null) {
                // Either reclip isn't configured, or it failed/timed out — fall back to
                // on-device extraction so this still works without the optional server.
                val fallbackTmp = File(context.cacheDir, "soniloko_yt_${System.currentTimeMillis()}.m4a")
                if (YoutubeRepository.downloadAudio(result.videoId, fallbackTmp)) {
                    finalFile = fallbackTmp
                }
            }

            downloadingId = null
            val file = finalFile
            if (file == null) {
                errorMsg = context.getString(R.string.youtube_error)
                return@launch
            }
            downloadedFile = file
            durationMs = AudioTrim.loadDurationMs(file)
            trimRange = 0f..1f
            name = result.title.take(40)
            stage = YtStage.READY
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(300.dp).heightIn(max = 560.dp)) {
                Text(stringResource(R.string.online_search_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                when (stage) {
                    YtStage.SEARCH -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            placeholder = { Text(stringResource(R.string.youtube_search_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { doSearch() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.search_action))
                        }
                        if (searching) {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        errorMsg?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        if (results.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SortChip(stringResource(R.string.sort_duration_asc), sortOrder == SortOrder.DURATION_ASC) { sortOrder = SortOrder.DURATION_ASC }
                                SortChip(stringResource(R.string.sort_duration_desc), sortOrder == SortOrder.DURATION_DESC) { sortOrder = SortOrder.DURATION_DESC }
                                SortChip(stringResource(R.string.sort_relevance), sortOrder == SortOrder.RELEVANCE) { sortOrder = SortOrder.RELEVANCE }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(visibleResults, key = { it.videoId }) { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            r.uploader + (if (r.durationSec > 0) " • " + formatDuration(r.durationSec) else ""),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    TextButton(onClick = { togglePreview(r) }) {
                                        Text(if (previewingId == r.videoId) stringResource(R.string.stop_recording) else stringResource(R.string.play_recording))
                                    }
                                    if (downloadingId == r.videoId) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    } else {
                                        TextButton(onClick = { startDownload(r) }) { Text(stringResource(R.string.download)) }
                                    }
                                }
                                HorizontalDivider()
                            }
                            if (visibleCount < sortedResults.size) {
                                item {
                                    TextButton(
                                        onClick = { visibleCount += PAGE_SIZE },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.load_more_results))
                                    }
                                }
                            }
                        }
                    }

                    YtStage.READY -> {
                        if (durationMs > 0 && downloadedFile != null) {
                            TrimControls(downloadedFile!!, durationMs, trimRange) { trimRange = it }
                            Spacer(Modifier.height(12.dp))
                        }
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
                    if (stage == YtStage.READY) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = name.isNotBlank(),
                            onClick = {
                                stopPreview()
                                val file = downloadedFile ?: return@Button
                                val isFullRange = trimRange.start <= 0.001f && trimRange.endInclusive >= 0.999f
                                val finalFile = if (isFullRange || durationMs <= 0) {
                                    file
                                } else {
                                    val startMs = (trimRange.start * durationMs).toLong()
                                    val endMs = (trimRange.endInclusive * durationMs).toLong()
                                    AudioTrim.trim(file, startMs, endMs) ?: file
                                }
                                onSaved(name.trim(), finalFile)
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatDuration(totalSec: Long): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
