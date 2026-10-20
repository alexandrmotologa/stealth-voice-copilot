package com.copilot.stealth.data.repository

import android.content.Context
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.EngineType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persistent storage and retrieval of carrier profile, Groq configuration,
 * and user preferences for primary and local neural engines.
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

    fun getGroqApiKey(): String {
        val stored = prefs.getString(KEY_GROQ_KEY, null)
        // Disregard old or invalid cached keys to ensure new Groq key from BuildConfig is active
        if (!stored.isNullOrBlank() && !stored.startsWith("gsk_flKkUxk8")) {
            return stored
        }
        return com.copilot.stealth.BuildConfig.DEFAULT_GROQ_API_KEY
    }

    fun saveGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_KEY, key.trim()).apply()
    }

    fun getCloudModel(): String {
        val stored = prefs.getString(KEY_CLOUD_MODEL, null)
        if (stored.isNullOrBlank() || stored == "openai/gpt-oss-20b") {
            return DEFAULT_CLOUD_MODEL
        }
        return stored
    }

    fun saveCloudModel(model: String) {
        prefs.edit().putString(KEY_CLOUD_MODEL, model.trim()).apply()
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
        prefs.edit().putString(KEY_ENGINE_TYPE, engineType.name).apply()
    }

    fun getPreferredLocalModel(): String {
        return prefs.getString(KEY_PREFERRED_LOCAL_MODEL, "Llama 3.2 1B") ?: "Llama 3.2 1B"
    }

    fun savePreferredLocalModel(modelName: String) {
        prefs.edit().putString(KEY_PREFERRED_LOCAL_MODEL, modelName).apply()
    }

    fun isInitialSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, true)
    }

    fun setInitialSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "stealth_carrier_prefs"
        private const val KEY_PROFILE = "carrier_profile_data"
        private const val KEY_GROQ_KEY = "groq_api_key"
        private const val KEY_CLOUD_MODEL = "cloud_model_name"
        private const val KEY_ENGINE_TYPE = "active_engine_type"
        private const val KEY_PREFERRED_LOCAL_MODEL = "preferred_local_model"
        private const val KEY_SETUP_COMPLETED = "initial_engine_setup_completed"

        const val DEFAULT_GROQ_KEY = ""
        const val DEFAULT_CLOUD_MODEL = "qwen/qwen3.8-27b"
    }
}
