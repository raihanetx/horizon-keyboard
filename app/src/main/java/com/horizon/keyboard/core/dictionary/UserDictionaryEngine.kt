package com.horizon.keyboard.core.dictionary

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * User dictionary engine — learns words the user types and persists them.
 *
 * Every word the user completes (via space or enter) is recorded.
 * Frequently used words get higher frequency scores and appear in suggestions.
 *
 * Storage: `user_dictionary.json` in app internal storage.
 */
class UserDictionaryEngine {

    companion object {
        private const val TAG = "UserDictionaryEngine"
        private const val FILE_NAME = "user_dictionary.json"
    }

    /** User word frequencies. */
    private val words = mutableMapOf<String, Int>()

    /** Whether data has been loaded. */
    val isLoaded: Boolean get() = words.isNotEmpty()

    /** Number of user-learned words. */
    val size: Int get() = words.size

    // ─── Loading & Saving ────────────────────────────────────────

    /**
     * Load user dictionary from JSON file. Call once on app startup.
     *
     * @param context Android context for file access.
     * @return Number of words loaded.
     */
    fun load(context: Context): Int {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                Log.i(TAG, "No user dictionary found, starting fresh")
                return 0
            }

            val json = file.readText()
            val obj = JSONObject(json)

            obj.keys().forEach { word ->
                val freq = obj.optInt(word, 0)
                if (freq > 0) {
                    words[word.lowercase()] = freq
                }
            }

            Log.i(TAG, "Loaded ${words.size} user words")
            words.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user dictionary", e)
            0
        }
    }

    /**
     * Save user dictionary to JSON file.
     *
     * @param context Android context for file access.
     * @return true if saved successfully.
     */
    fun save(context: Context): Boolean {
        return try {
            val obj = JSONObject()
            words.forEach { (word, freq) ->
                obj.put(word, freq)
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(obj.toString(2)) // Pretty print with indent

            Log.d(TAG, "Saved ${words.size} user words")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user dictionary", e)
            false
        }
    }

    // ─── Word Learning ───────────────────────────────────────────

    /**
     * Record a word the user typed. Increments frequency if already known.
     *
     * Call this every time the user completes a word (space, enter, punctuation).
     *
     * @param word The word to add/increment.
     */
    fun addWord(word: String) {
        val lower = word.lowercase().trim()
        if (lower.length < 2) return // Skip single chars
        if (lower.all { !it.isLetter() }) return // Skip non-alpha

        words[lower] = (words[lower] ?: 0) + 1
    }

    /**
     * Get the frequency of a user-learned word.
     *
     * @param word The word to look up.
     * @return Frequency count, or 0 if not learned yet.
     */
    fun getFrequency(word: String): Int {
        return words[word.lowercase()] ?: 0
    }

    /**
     * Check if a word has been learned.
     */
    fun contains(word: String): Boolean {
        return words.containsKey(word.lowercase())
    }

    // ─── Suggestions ─────────────────────────────────────────────

    /**
     * Get user's words matching a prefix, ranked by frequency.
     *
     * @param prefix The typed prefix.
     * @param limit Maximum suggestions.
     * @return Matching words sorted by frequency (highest first).
     */
    fun getSuggestions(prefix: String, limit: Int = 5): List<String> {
        if (prefix.isEmpty()) return emptyList()

        val lower = prefix.lowercase()

        return words.entries
            .filter { it.key.startsWith(lower) && it.key != lower }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Get the user's most frequently used words.
     *
     * @param limit Number of words to return.
     * @return Top words by frequency.
     */
    fun getTopWords(limit: Int = 10): List<String> {
        return words.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Remove a word from the user dictionary.
     */
    fun removeWord(word: String) {
        words.remove(word.lowercase())
    }

    /**
     * Clear all user-learned words.
     */
    fun clear() {
        words.clear()
    }
}
