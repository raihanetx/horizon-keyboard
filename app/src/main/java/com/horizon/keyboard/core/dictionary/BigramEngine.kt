package com.horizon.keyboard.core.dictionary

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Bigram engine — learns word pairs and predicts next words.
 *
 * Tracks which words commonly follow other words.
 * Example: user types "good morning" 10 times → after typing "good",
 * "morning" becomes the #1 suggestion.
 *
 * Storage: `user_bigrams.json` in app internal storage.
 */
class BigramEngine {

    companion object {
        private const val TAG = "BigramEngine"
        private const val FILE_NAME = "user_bigrams.json"
    }

    /**
     * Bigram storage: word1 → (word2 → count)
     * Example: "good" → {"morning": 10, "job": 5, "night": 3}
     */
    private val bigrams = mutableMapOf<String, MutableMap<String, Int>>()

    /** Whether data has been loaded. */
    val isLoaded: Boolean get() = bigrams.isNotEmpty()

    /** Total number of bigram pairs tracked. */
    val size: Int get() = bigrams.values.sumOf { it.size }

    // ─── Loading & Saving ────────────────────────────────────────

    /**
     * Load bigrams from JSON file. Call once on app startup.
     *
     * @param context Android context for file access.
     * @return Number of bigram pairs loaded.
     */
    fun load(context: Context): Int {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                Log.i(TAG, "No bigram data found, starting fresh")
                return 0
            }

            val json = file.readText()
            val obj = JSONObject(json)

            var count = 0
            obj.keys().forEach { word1 ->
                val followers = obj.optJSONObject(word1) ?: return@forEach
                val map = mutableMapOf<String, Int>()
                followers.keys().forEach { word2 ->
                    val freq = followers.optInt(word2, 0)
                    if (freq > 0) {
                        map[word2] = freq
                        count++
                    }
                }
                if (map.isNotEmpty()) {
                    bigrams[word1.lowercase()] = map
                }
            }

            Log.i(TAG, "Loaded $count bigram pairs")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bigrams", e)
            0
        }
    }

    /**
     * Save bigrams to JSON file.
     *
     * @param context Android context for file access.
     * @return true if saved successfully.
     */
    fun save(context: Context): Boolean {
        return try {
            val obj = JSONObject()
            bigrams.forEach { (word1, followers) ->
                val followersObj = JSONObject()
                followers.forEach { (word2, freq) ->
                    followersObj.put(word2, freq)
                }
                obj.put(word1, followersObj)
            }

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(obj.toString(2))

            Log.d(TAG, "Saved $size bigram pairs")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bigrams", e)
            false
        }
    }

    // ─── Learning ────────────────────────────────────────────────

    /**
     * Record a word pair. Increments count if pair already exists.
     *
     * Call this every time the user completes a word — pass the previous
     * word and the new word.
     *
     * @param word1 The previous word (context).
     * @param word2 The word that followed.
     */
    fun addPair(word1: String, word2: String) {
        val w1 = word1.lowercase().trim()
        val w2 = word2.lowercase().trim()

        if (w1.length < 2 || w2.length < 2) return
        if (w1.all { !it.isLetter() } || w2.all { !it.isLetter() }) return

        val followers = bigrams.getOrPut(w1) { mutableMapOf() }
        followers[w2] = (followers[w2] ?: 0) + 1
    }

    // ─── Prediction ──────────────────────────────────────────────

    /**
     * Get likely next words after a given word.
     *
     * @param word The word the user just typed.
     * @param limit Maximum suggestions.
     * @return List of likely next words, sorted by frequency (highest first).
     */
    fun getNextWords(word: String, limit: Int = 3): List<String> {
        if (word.isEmpty()) return emptyList()

        val followers = bigrams[word.lowercase()] ?: return emptyList()

        return followers.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Get the count of a specific bigram pair.
     *
     * @return How many times word2 followed word1, or 0.
     */
    fun getPairCount(word1: String, word2: String): Int {
        return bigrams[word1.lowercase()]?.get(word2.lowercase()) ?: 0
    }

    /**
     * Check if a bigram pair exists.
     */
    fun hasPair(word1: String, word2: String): Boolean {
        return getPairCount(word1, word2) > 0
    }

    /**
     * Remove all bigrams starting with a specific word.
     */
    fun removeWord(word: String) {
        bigrams.remove(word.lowercase())
    }

    /**
     * Clear all bigram data.
     */
    fun clear() {
        bigrams.clear()
    }
}
