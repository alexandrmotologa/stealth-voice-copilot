package com.copilot.stealth.data.repository

import android.content.Context
import com.copilot.stealth.data.models.CarrierProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persistent storage and retrieval of the candidate's carrier profile.
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
        if (!stored.isNullOrBlank()) {
            return stored
        }
        return com.copilot.stealth.BuildConfig.DEFAULT_GROQ_API_KEY
    }

    fun saveGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_KEY, key.trim()).apply()
    }

    fun getCloudModel(): String {
        return prefs.getString(KEY_CLOUD_MODEL, DEFAULT_CLOUD_MODEL) ?: DEFAULT_CLOUD_MODEL
    }

    fun saveCloudModel(model: String) {
        prefs.edit().putString(KEY_CLOUD_MODEL, model.trim()).apply()
    }

    companion object {
        private const val PREFS_NAME = "stealth_carrier_prefs"
        private const val KEY_PROFILE = "carrier_profile_data"
        private const val KEY_GROQ_KEY = "groq_api_key"
        private const val KEY_CLOUD_MODEL = "cloud_model_name"

        const val DEFAULT_GROQ_KEY = ""
        const val DEFAULT_CLOUD_MODEL = "openai/gpt-oss-20b"
    }
}
