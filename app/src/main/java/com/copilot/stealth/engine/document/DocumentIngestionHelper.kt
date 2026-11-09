package com.copilot.stealth.engine.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

data class ExtractedDocument(
    val fileName: String,
    val text: String,
    val charCount: Int,
    val wordCount: Int
)

/**
 * High-performance, offline document and CV/Resume ingestion helper.
 * Extracts plain text from PDF, TXT, Markdown, and CSV files directly on-device
 * using PDFBox-Android and native UTF-8 streaming, ensuring zero data leakage.
 */
object DocumentIngestionHelper {

    private const val TAG = "DocumentIngestion"
    private const val MAX_GROUNDING_CHARS = 16000

    fun extractTextFromUri(context: Context, uri: Uri): Result<ExtractedDocument> {
        return runCatching {
            val fileName = queryFileName(context, uri) ?: "document_${System.currentTimeMillis()}"
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || mimeType.contains("pdf", ignoreCase = true)

            val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
                if (isPdf) {
                    extractFromPdfStream(stream)
                } else {
                    extractFromTextStream(stream)
                }
            } ?: throw IllegalStateException("Unable to open stream for URI: $uri")

            val cleanText = sanitizeExtractedText(rawText)
            val words = cleanText.split(Regex("\\s+")).count { it.isNotBlank() }

            Log.i(TAG, "Ingested document '$fileName': ${cleanText.length} chars, $words words")
            ExtractedDocument(
                fileName = fileName,
                text = cleanText,
                charCount = cleanText.length,
                wordCount = words
            )
        }
    }

    private fun extractFromPdfStream(stream: InputStream): String {
        return try {
            val document = PDDocument.load(stream)
            try {
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = document.numberOfPages
                }
                stripper.getText(document)
            } finally {
                document.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDFBox extraction failed: ${e.message}", e)
            throw e
        }
    }

    private fun extractFromTextStream(stream: InputStream): String {
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun sanitizeExtractedText(text: String): String {
        val normalized = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            // Collapse 3+ newlines to 2
            .replace(Regex("\n{3,}"), "\n\n")
            // Remove null characters or weird binary artifacts
            .replace("\u0000", "")
            .trim()

        return if (normalized.length > MAX_GROUNDING_CHARS) {
            normalized.substring(0, MAX_GROUNDING_CHARS) + "\n\n[... Resume truncated for optimal AI memory context ...]"
        } else {
            normalized
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve file name from content resolver: ${e.message}")
            }
        }
        return uri.lastPathSegment
    }
}
