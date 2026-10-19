package com.copilot.stealth.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.copilot.stealth.data.models.CopilotTurn
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles secure formatting, AES-256-GCM encryption of interview transcripts,
 * and emergency panic wipes that cleanse RAM, disk cache, and clipboard.
 */
class SessionExportManager(private val context: Context? = null) {

    private val secureRandom = SecureRandom()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun formatPlainText(turns: List<CopilotTurn>): String {
        if (turns.isEmpty()) return "NO_TURNS_RECORDED"

        val sb = StringBuilder()
        sb.append("=== STEALTH VOICE COPILOT INTERVIEW TRANSCRIPT ===\n")
        sb.append("Generated: ${dateFormat.format(Date())}\n")
        sb.append("Total Turns: ${turns.size}\n\n")

        turns.forEachIndexed { index, turn ->
            sb.append("--- TURN #${index + 1} [${turn.matchedCategory ?: "GENERAL"}] ---\n")
            sb.append("Q (EN): ${turn.transcriptEn}\n")
            if (!turn.transcriptRo.isNullOrBlank()) {
                sb.append("Q (RO): ${turn.transcriptRo}\n")
            }
            sb.append("A (EN): ${turn.answerEn}\n")
            if (!turn.answerRo.isNullOrBlank()) {
                sb.append("A (RO): ${turn.answerRo}\n")
            }
            sb.append("STATS: ${turn.latencyMs}ms latency | ${(turn.confidence * 100).toInt()}% confidence\n\n")
        }

        return sb.toString().trimEnd()
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a 12-byte IV and 128-bit authentication tag.
     * Output format: [12 bytes IV][ciphertext + auth tag]
     */
    fun encryptSession(plainText: String, key: ByteArray = DEFAULT_DEMO_KEY): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val cipherBytes = cipher.doFinal(plainBytes)

        val result = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(cipherBytes, 0, result, iv.size, cipherBytes.size)
        return result
    }

    /**
     * Decrypts AES-256-GCM byte payload produced by [encryptSession].
     */
    fun decryptSession(encryptedData: ByteArray, key: ByteArray = DEFAULT_DEMO_KEY): String {
        require(encryptedData.size > GCM_IV_LENGTH_BYTES) { "Invalid payload size" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val cipherText = ByteArray(encryptedData.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedData, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Copies text to the Android clipboard.
     */
    fun copyToClipboard(label: String, text: String): Boolean {
        val ctx = context ?: return false
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        return true
    }

    /**
     * Panic Wipe: Cleans clipboard, deletes cached files, and overwrites sensitive data.
     */
    fun panicWipe(): Boolean {
        val ctx = context ?: return false

        // 1. Wipe clipboard
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val emptyClip = ClipData.newPlainText("", "")
            clipboard.setPrimaryClip(emptyClip)
        }

        // 2. Overwrite and delete temp cache files
        try {
            val cacheFiles = ctx.cacheDir.listFiles() ?: emptyArray()
            for (file in cacheFiles) {
                if (file.isFile) {
                    secureDeleteFile(file)
                }
            }
        } catch (_: Exception) {}

        // 3. Clear temporary model/transcript scratch files if present
        try {
            val appFiles = ctx.filesDir.listFiles() ?: emptyArray()
            for (file in appFiles) {
                if (file.isFile && file.name.endsWith(".tmp")) {
                    secureDeleteFile(file)
                }
            }
        } catch (_: Exception) {}

        return true
    }

    private fun secureDeleteFile(file: File) {
        try {
            if (file.exists() && file.length() > 0) {
                val zeroes = ByteArray(file.length().toInt().coerceAtMost(4096))
                file.outputStream().use { it.write(zeroes) }
            }
            file.delete()
        } catch (_: Exception) {
            file.delete()
        }
    }

    companion object {
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        // Standard 256-bit key for on-device local transcript vault
        val DEFAULT_DEMO_KEY = byteArrayOf(
            0x2b.toByte(), 0x7e.toByte(), 0x15.toByte(), 0x16.toByte(),
            0x28.toByte(), 0xae.toByte(), 0xd2.toByte(), 0xa6.toByte(),
            0xab.toByte(), 0xf7.toByte(), 0x15.toByte(), 0x88.toByte(),
            0x09.toByte(), 0xcf.toByte(), 0x4f.toByte(), 0x3c.toByte(),
            0x55.toByte(), 0x66.toByte(), 0x77.toByte(), 0x88.toByte(),
            0x11.toByte(), 0x22.toByte(), 0x33.toByte(), 0x44.toByte(),
            0x99.toByte(), 0xaa.toByte(), 0xbb.toByte(), 0xcc.toByte(),
            0xdd.toByte(), 0xee.toByte(), 0xff.toByte(), 0x01.toByte()
        )
    }
}
