package com.copilot.stealth.engine.llm

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Model specification descriptor for downloadable on-device neural weights.
 */
data class ModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeDescription: String,
    val architecture: String,
    val downloadUrl: String
)

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus
    data object Completed : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

/**
 * Manages downloading and tracking of Llama 3.2 and Gemma 2 on-device neural models.
 */
class ModelDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    private val activeDownloads = mutableMapOf<String, Long>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    companion object {
        val LLAMA_3_2_1B = ModelSpec(
            id = "llama_3_2_1b",
            displayName = "Llama 3.2 1B Instruct",
            fileName = "llama-3.2-1b-instruct.bin",
            sizeDescription = "~1.2 GB",
            architecture = "Meta Llama (Fast & Conversational)",
            downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/resolve/main/llama-3.2-1b-instruct.bin"
        )

        val GEMMA_2_2B = ModelSpec(
            id = "gemma_2_2b",
            displayName = "Gemma 2 2B IT",
            fileName = "gemma-2b-it-gpu-int4.bin",
            sizeDescription = "~1.35 GB",
            architecture = "Google Gemma (High Reasoning)",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin"
        )

        val ALL_MODELS = listOf(LLAMA_3_2_1B, GEMMA_2_2B)
    }

    fun isModelDownloaded(spec: ModelSpec): Boolean {
        return getModelFile(spec) != null
    }

    fun getModelFile(spec: ModelSpec): File? {
        val internalDir = File(context.filesDir, "models")
        val internalFile = File(internalDir, spec.fileName)
        if (internalFile.exists() && internalFile.length() > 50_000_000) {
            return internalFile
        }

        val externalFiles = context.getExternalFilesDir("models")
        if (externalFiles != null) {
            val extFile = File(externalFiles, spec.fileName)
            if (extFile.exists() && extFile.length() > 50_000_000) {
                return extFile
            }
        }

        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null && publicDownloads.exists()) {
            val dlFile = File(publicDownloads, spec.fileName)
            if (dlFile.exists() && dlFile.length() > 50_000_000) {
                return dlFile
            }
            // Check variations like .task, .bin or with prefix
            val fuzzyMatch = publicDownloads.listFiles()?.firstOrNull { f ->
                f.isFile && f.length() > 50_000_000 &&
                        (f.name.contains(spec.id, ignoreCase = true) ||
                         f.name.contains(spec.displayName.substringBefore(" "), ignoreCase = true))
            }
            if (fuzzyMatch != null) {
                return fuzzyMatch
            }
        }

        return null
    }

    fun startDownload(spec: ModelSpec): Boolean {
        val dm = downloadManager ?: return false

        try {
            val uri = Uri.parse(spec.downloadUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Downloading ${spec.displayName}")
                setDescription("Local neural model for Stealth Voice Copilot (${spec.sizeDescription})")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, spec.fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val downloadId = dm.enqueue(request)
            activeDownloads[spec.id] = downloadId
            _downloadStates.update { current ->
                current + (spec.id to DownloadStatus.Downloading(0, 0, 0))
            }
            return true
        } catch (e: Exception) {
            _downloadStates.update { current ->
                current + (spec.id to DownloadStatus.Error(e.message ?: "Download failed"))
            }
            return false
        }
    }
}
