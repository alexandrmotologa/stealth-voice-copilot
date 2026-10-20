package com.copilot.stealth.engine.llm

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Model metadata descriptor for on-device neural weights.
 */
data class AvailableModelInfo(
    val name: String,
    val file: File,
    val sizeBytes: Long,
    val format: String,
    val isRecommended: Boolean = false
) {
    val sizeMb: Double
        get() = sizeBytes / (1024.0 * 1024.0)

    val formattedSize: String
        get() = String.format(java.util.Locale.US, "%.1f MB", sizeMb)
}

/**
 * Discovers and organizes on-device quantized neural models across internal and shared storage.
 */
class LocalModelManager(private val context: Context) {

    private val supportedExtensions = listOf("bin", "task", "onnx", "tflite")

    fun getModelsDirectory(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Scans internal app storage and the Downloads folder for compatible LLM model files.
     */
    fun scanAvailableModels(): List<AvailableModelInfo> {
        val results = mutableListOf<AvailableModelInfo>()
        val searchDirs = listOfNotNull(
            getModelsDirectory(),
            context.getExternalFilesDir("models"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && supportedExtensions.any { file.name.endsWith(".$it", ignoreCase = true) }) {
                        val extension = file.extension.uppercase()
                        val isRec = file.name.contains("llama", ignoreCase = true) ||
                                file.name.contains("gemma", ignoreCase = true) ||
                                file.name.contains("smol", ignoreCase = true)

                        results.add(
                            AvailableModelInfo(
                                name = file.name,
                                file = file,
                                sizeBytes = file.length(),
                                format = extension,
                                isRecommended = isRec
                            )
                        )
                    }
                }
            }
        }

        return results.distinctBy { it.file.absolutePath }.sortedByDescending { it.sizeBytes }
    }

    fun getLlamaModel(): File? {
        val models = scanAvailableModels().map { it.file }
        return models.firstOrNull { it.name.contains("llama", ignoreCase = true) }
    }

    fun getGemmaModel(): File? {
        val models = scanAvailableModels().map { it.file }
        return models.firstOrNull { it.name.contains("gemma", ignoreCase = true) }
    }

    fun getModelByPreference(preferredName: String): File? {
        return if (preferredName.contains("gemma", ignoreCase = true)) {
            getGemmaModel() ?: getLlamaModel() ?: getDefaultModel()
        } else {
            getLlamaModel() ?: getGemmaModel() ?: getDefaultModel()
        }
    }

    /**
     * Returns the primary or default model candidate if present.
     */
    fun getDefaultModel(): File? {
        val defaultInternal = File(getModelsDirectory(), "llama-3.2-1b-instruct-q4.bin")
        if (defaultInternal.exists() && defaultInternal.length() > 50_000_000) {
            return defaultInternal
        }

        return scanAvailableModels().firstOrNull()?.file
    }
}
