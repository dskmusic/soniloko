package com.dsk.soniloko.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.data.image.ImageSearchRepository
import com.dsk.soniloko.data.image.ImageSearchResult
import com.dsk.soniloko.data.image.ImageSearchType
import kotlinx.coroutines.launch
import java.io.File

private const val IMAGE_SEARCH_PAGE_SIZE = 30

/**
 * Search + pick an online image (Pixabay, requires a free API key — see BuildConfig.PIXABAY_API_KEY).
 * Picking a thumbnail downloads the full-size image and runs it through [ImageCropDialog] right
 * here (rather than bubbling a Uri up to the caller), so cancelling the crop just returns to the
 * results grid with the search still intact instead of closing the whole flow.
 */
@Composable
fun ImageSearchDialog(onDismiss: () -> Unit, onImageReady: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // LocalSoftwareKeyboardController.hide() is unreliable from inside a Compose Dialog's own
    // window on some devices/OEM skins — fall back to driving the IME directly via
    // InputMethodManager against this view's window token, which reliably dismisses it.
    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    var query by remember { mutableStateOf("") }
    var imageType by remember { mutableStateOf(ImageSearchType.ILLUSTRATION) }
    var results by remember { mutableStateOf<List<ImageSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    fun doSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        hideKeyboard()
        searching = true
        errorMsg = null
        scope.launch {
            val r = ImageSearchRepository.search(q, imageType, page = 1)
            searching = false
            results = r
            page = 1
            hasMore = r.size >= IMAGE_SEARCH_PAGE_SIZE
            if (r.isEmpty()) errorMsg = context.getString(R.string.youtube_no_results)
        }
    }

    fun loadMore() {
        val q = query.trim()
        if (q.isBlank() || loadingMore || searching) return
        loadingMore = true
        val nextPage = page + 1
        scope.launch {
            val r = ImageSearchRepository.search(q, imageType, page = nextPage)
            loadingMore = false
            hasMore = r.size >= IMAGE_SEARCH_PAGE_SIZE
            if (r.isNotEmpty()) {
                // Photo/vector searches often have a much smaller result pool than illustrations,
                // so paging past the end can make Pixabay repeat earlier hits instead of returning
                // empty — duplicate ids in the grid's `items(key = { it.id })` crash with
                // "Key ... was already used". Drop anything already shown.
                val existingIds = results.mapTo(mutableSetOf()) { it.id }
                val newResults = r.filter { it.id !in existingIds }
                hasMore = hasMore && newResults.isNotEmpty()
                results = results + newResults
                page = nextPage
            }
        }
    }

    fun pick(result: ImageSearchResult) {
        if (downloadingId != null) return
        downloadingId = result.id
        errorMsg = null
        scope.launch {
            val bytes = ImageSearchRepository.downloadBytes(result.fullUrl)
                ?: ImageSearchRepository.downloadBytes(result.thumbnailUrl)
            downloadingId = null
            if (bytes == null) {
                errorMsg = context.getString(R.string.youtube_error)
                return@launch
            }
            val file = File(context.cacheDir, "soniloko_img_${System.currentTimeMillis()}.jpg")
            runCatching { file.writeBytes(bytes) }
                .onSuccess { pendingImageUri = Uri.fromFile(file) }
                .onFailure { errorMsg = context.getString(R.string.youtube_error) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(320.dp).heightIn(max = 520.dp)) {
                Text(stringResource(R.string.search_image_online), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.image_search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeChip(stringResource(R.string.image_type_illustration), imageType == ImageSearchType.ILLUSTRATION) {
                        imageType = ImageSearchType.ILLUSTRATION
                        if (query.isNotBlank()) doSearch()
                    }
                    TypeChip(stringResource(R.string.image_type_photo), imageType == ImageSearchType.PHOTO) {
                        imageType = ImageSearchType.PHOTO
                        if (query.isNotBlank()) doSearch()
                    }
                    TypeChip(stringResource(R.string.image_type_vector), imageType == ImageSearchType.VECTOR) {
                        imageType = ImageSearchType.VECTOR
                        if (query.isNotBlank()) doSearch()
                    }
                    TypeChip(stringResource(R.string.image_type_all), imageType == ImageSearchType.ALL) {
                        imageType = ImageSearchType.ALL
                        if (query.isNotBlank()) doSearch()
                    }
                }
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

                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f, fill = false)) {
                    items(results, key = { it.id }) { result ->
                        ImageThumbnail(
                            result = result,
                            downloading = downloadingId == result.id,
                            onClick = { pick(result) }
                        )
                    }
                }
                if (hasMore && results.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { loadMore() }, enabled = !loadingMore, modifier = Modifier.fillMaxWidth()) {
                        if (loadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(stringResource(R.string.load_more_results))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }

    pendingImageUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onDismiss = { pendingImageUri = null },
            onCropped = { bitmap ->
                pendingImageUri = null
                onImageReady(bitmap)
            }
        )
    }
}

@Composable
private fun ImageThumbnail(result: ImageSearchResult, downloading: Boolean, onClick: () -> Unit) {
    var bitmap by remember(result.thumbnailUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(result.thumbnailUrl) {
        val bytes = ImageSearchRepository.downloadBytes(result.thumbnailUrl)
        bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    Box(
        Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !downloading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (downloading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
