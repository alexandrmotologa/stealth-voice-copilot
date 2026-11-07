package com.copilot.stealth.data.repository

import android.content.Context
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.EngineType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persistent storage and retrieval of carrier profile, Gemini (Primary + Backup),
 * Groq cloud configuration, and user preferences for primary and local neural engines.
 */
class CarrierProfileRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getProfile(): CarrierProfile {
        val serialized = prefs.getString(KEY_PROFILE, null) ?: return CarrierProfile()
        return try {
            json.decodeFromString<CarrierProfile>(serialized)
        } catch (_: Exception) {
            CarrierProfile()
        }
    }

    fun saveProfile(profile: CarrierProfile) {
        val serialized = json.encodeToString(profile)
        prefs.edit().putString(KEY_PROFILE, serialized).apply()
    }

    // Google AI Studio Gemini - Primary API Key
    fun getGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_KEY, "") ?: ""
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, key.trim()).commit()
    }

    // Google AI Studio Gemini - Backup / Fallback API Key (doubles quotas or provides redundant failover)
    fun getGeminiBackupApiKey(): String {
        return prefs.getString(KEY_GEMINI_BACKUP_KEY, "") ?: ""
    }

    fun saveGeminiBackupApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_BACKUP_KEY, key.trim()).commit()
    }

    // Gemini Primary Model (default: gemini-2.5-flash)
    fun getGeminiModel(): String {
        return prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
    }

    fun saveGeminiModel(model: String) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model.trim()).commit()
    }

    // Gemini Fallback Model (default: gemini-2.0-flash)
    fun getGeminiFallbackModel(): String {
        return prefs.getString(KEY_GEMINI_FALLBACK_MODEL, DEFAULT_GEMINI_FALLBACK_MODEL) ?: DEFAULT_GEMINI_FALLBACK_MODEL
    }

    fun saveGeminiFallbackModel(model: String) {
        prefs.edit().putString(KEY_GEMINI_FALLBACK_MODEL, model.trim()).commit()
    }

    // Groq API Key
    fun getGroqApiKey(): String {
        val stored = prefs.getString(KEY_GROQ_KEY, null)
        if (!stored.isNullOrBlank() && !stored.startsWith("gsk_flKkUxk8")) {
            return stored
        }
        return ""
    }

    fun saveGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_KEY, key.trim()).commit()
    }

    // Groq Model (default: openai/gpt-oss-20b or llama-3.3-70b-versatile)
    fun getCloudModel(): String {
        val stored = prefs.getString(KEY_CLOUD_MODEL, null)
        return if (!stored.isNullOrBlank()) stored else DEFAULT_CLOUD_MODEL
    }

    fun saveCloudModel(model: String) {
        prefs.edit().putString(KEY_CLOUD_MODEL, model.trim()).commit()
    }

    // STT Language Mode (default: ENGLISH)
    fun getSttLanguageMode(): com.copilot.stealth.data.models.SttLanguageMode {
        val name = prefs.getString(KEY_STT_LANGUAGE_MODE, com.copilot.stealth.data.models.SttLanguageMode.ENGLISH.name)
        return try {
            com.copilot.stealth.data.models.SttLanguageMode.valueOf(name ?: com.copilot.stealth.data.models.SttLanguageMode.ENGLISH.name)
        } catch (_: Exception) {
            com.copilot.stealth.data.models.SttLanguageMode.ENGLISH
        }
    }

    fun saveSttLanguageMode(mode: com.copilot.stealth.data.models.SttLanguageMode) {
        prefs.edit().putString(KEY_STT_LANGUAGE_MODE, mode.name).apply()
    }

    // Translation & Question Answering Preference
    fun isQuestionAutoAnswerEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_ANSWER_QUESTIONS, true)
    }

    fun saveQuestionAutoAnswerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ANSWER_QUESTIONS, enabled).apply()
    }

    fun getEngineType(): EngineType {
        val name = prefs.getString(KEY_ENGINE_TYPE, EngineType.HYBRID.name)
        return try {
            EngineType.valueOf(name ?: EngineType.HYBRID.name)
        } catch (_: Exception) {
            EngineType.HYBRID
        }
    }

    fun saveEngineType(engineType: EngineType) {
        prefs.edit().putString(KEY_ENGINE_TYPE, engineType.name).commit()
    }

    fun getPreferredLocalModel(): String {
        return prefs.getString(KEY_PREFERRED_LOCAL_MODEL, "Llama 3.2 1B") ?: "Llama 3.2 1B"
    }

    fun savePreferredLocalModel(modelName: String) {
        prefs.edit().putString(KEY_PREFERRED_LOCAL_MODEL, modelName).commit()
    }

    fun isSpeakingCooldownEnabled(): Boolean {
        return prefs.getBoolean(KEY_SPEAKING_COOLDOWN_ENABLED, false)
    }

    fun saveSpeakingCooldownEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPEAKING_COOLDOWN_ENABLED, enabled).commit()
    }

    fun isInitialSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, true)
    }

    fun setInitialSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).commit()
    }

    companion object {
        private const val PREFS_NAME = "stealth_carrier_prefs"
        private const val KEY_PROFILE = "carrier_profile_data"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_GEMINI_BACKUP_KEY = "gemini_backup_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model_name"
        private const val KEY_GEMINI_FALLBACK_MODEL = "gemini_fallback_model_name"
        private const val KEY_GROQ_KEY = "groq_api_key"
        private const val KEY_CLOUD_MODEL = "cloud_model_name"
        private const val KEY_ENGINE_TYPE = "active_engine_type"
        private const val KEY_PREFERRED_LOCAL_MODEL = "preferred_local_model"
        private const val KEY_SETUP_COMPLETED = "initial_engine_setup_completed"
        private const val KEY_AUTO_ANSWER_QUESTIONS = "auto_answer_questions_enabled"
        private const val KEY_STT_LANGUAGE_MODE = "stt_language_mode"
        private const val KEY_SPEAKING_COOLDOWN_ENABLED = "speaking_cooldown_enabled"

        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_GEMINI_FALLBACK_MODEL = "gemini-2.0-flash"
        const val DEFAULT_GROQ_KEY = ""
        const val DEFAULT_CLOUD_MODEL = "llama-3.3-70b-versatile"
    }
}
