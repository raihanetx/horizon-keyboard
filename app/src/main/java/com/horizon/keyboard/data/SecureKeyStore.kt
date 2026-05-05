package com.horizon.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure API Key Storage using Android Keystore + EncryptedSharedPreferences.
 *
 * Security layers:
 * 1. Android Keystore — hardware-backed key storage (TEE/StrongBox on supported devices)
 * 2. AES-256-GCM encryption — keys encrypted at rest in SharedPreferences
 * 3. MasterKey never exported — lives only in Keystore, inaccessible to other apps
 *
 * This is the standard Android approach for storing credentials.
 * Keys survive app updates but are cleared on app uninstall.
 */
object SecureKeyStore {

    private const val PREFS_FILE = "horizon_secure_keys"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Initialize the encrypted storage. Call once from Application or Service.
     * Thread-safe via lazy initialization.
     */
    fun init(context: Context): SharedPreferences {
        return getOrCreatePrefs(context)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Try to create encrypted prefs. If it fails (corrupted keystore, MIUI issues, etc.),
     * delete the corrupted file and retry once.
     */
    private fun getOrCreatePrefs(context: Context): SharedPreferences {
        prefs?.let { return it }
        return synchronized(this) {
            prefs?.let { return it }
            try {
                createEncryptedPrefs(context.applicationContext).also { prefs = it }
            } catch (e: Exception) {
                // Corrupted prefs — delete and retry
                try {
                    context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.applicationContext.deleteSharedPreferences(PREFS_FILE)
                } catch (_: Exception) {}
                createEncryptedPrefs(context.applicationContext).also { prefs = it }
            }
        }
    }

    // ─── Key Storage API ─────────────────────────────────────────

    fun putString(context: Context, key: String, value: String) {
        init(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        return init(context).getString(key, default) ?: default
    }

    fun hasKey(context: Context, key: String): Boolean {
        return init(context).contains(key) && getString(context, key).isNotEmpty()
    }

    fun removeKey(context: Context, key: String) {
        init(context).edit().remove(key).apply()
    }

    // ─── Convenience Methods for API Keys ────────────────────────

    private const val KEY_GROQ_API = "groq_api_key"
    private const val KEY_GEMMA_API = "gemma_api_key"

    fun setGroqKey(context: Context, key: String) = putString(context, KEY_GROQ_API, key)
    fun getGroqKey(context: Context): String = getString(context, KEY_GROQ_API)
    fun hasGroqKey(context: Context): Boolean = hasKey(context, KEY_GROQ_API)

    fun setGemmaKey(context: Context, key: String) = putString(context, KEY_GEMMA_API, key)
    fun getGemmaKey(context: Context): String = getString(context, KEY_GEMMA_API)
    fun hasGemmaKey(context: Context): Boolean = hasKey(context, KEY_GEMMA_API)
}
