package com.dsk.soniloko.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optional self-hosted "reclip" server (yt-dlp-based, same job-submit/poll/fetch API as
 * DSKGrab) tried ahead of the embedded NewPipeExtractor fallback. Since it auto-updates
 * itself server-side, it keeps working even if YouTube changes something NewPipeExtractor
 * hasn't caught up with yet. Disabled whenever [serverUrl] is blank.
 */
object ReclipRepository {
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Submits a download job for [videoId], polls until done (or [maxWaitMs] elapses), then
     * fetches the resulting audio file into [destFile]. Returns false on any failure/timeout. */
    suspend fun downloadAudio(
        serverUrl: String,
        username: String,
        password: String,
        videoId: String,
        title: String,
        destFile: File,
        maxWaitMs: Long = 45_000
    ): Boolean = withContext(Dispatchers.IO) {
        if (serverUrl.isBlank()) return@withContext false
        val base = serverUrl.trimEnd('/')
        val auth = Credentials.basic(username, password)

        runCatching {
            val payload = JSONObject().apply {
                put("url", "https://www.youtube.com/watch?v=$videoId")
                put("format", "audio")
                put("title", title)
            }
            val jobReq = Request.Builder()
                .url("$base/api/download")
                .addHeader("Authorization", auth)
                .post(payload.toString().toRequestBody(JSON_TYPE))
                .build()
            val jobResp = client().newCall(jobReq).execute()
            if (!jobResp.isSuccessful) return@withContext false
            val jobId = JSONObject(jobResp.body?.string().orEmpty()).optString("job_id")
            if (jobId.isBlank()) return@withContext false

            val deadline = System.currentTimeMillis() + maxWaitMs
            while (System.currentTimeMillis() < deadline) {
                delay(2_000)
                val statusReq = Request.Builder()
                    .url("$base/api/status/$jobId")
                    .addHeader("Authorization", auth)
                    .get()
                    .build()
                val statusResp = client().newCall(statusReq).execute()
                if (!statusResp.isSuccessful) continue
                val status = JSONObject(statusResp.body?.string().orEmpty())
                when (status.optString("status")) {
                    "done" -> {
                        val fileReq = Request.Builder()
                            .url("$base/api/file/$jobId")
                            .addHeader("Authorization", auth)
                            .get()
                            .build()
                        val fileResp = client().newCall(fileReq).execute()
                        if (!fileResp.isSuccessful) return@withContext false
                        val body = fileResp.body ?: return@withContext false
                        destFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                        return@withContext true
                    }
                    "error" -> return@withContext false
                    else -> { /* keep polling */ }
                }
            }
            false
        }.getOrDefault(false)
    }
}
