package com.horizon.keyboard.core.dictionary

import android.content.Context
import android.util.Log

/**
 * Core dictionary engine — loads word frequency data and provides prefix-based suggestions.
 *
 * Loads from `assets/dictionaries/en_frequency.txt` (format: `word:frequency` per line).
 * Words are stored in a sorted list for efficient prefix matching via binary search.
 *
 * Thread-safe: loaded once on initialization, read-only after that.
 */
class DictionaryEngine {

    companion object {
        private const val TAG = "DictionaryEngine"
        private const val DICT_FILE = "dictionaries/en_frequency.txt"
    }

    /** Word-frequency pairs, sorted alphabetically for binary search. */
    private var entries: List<Pair<String, Int>> = emptyList()

    /** Word-to-frequency lookup for O(1) access. */
    private var frequencyMap: Map<String, Int> = emptyMap()

    /** Whether the dictionary has been loaded. */
    val isLoaded: Boolean get() = entries.isNotEmpty()

    /** Total number of words in the dictionary. */
    val size: Int get() = entries.size

    // ─── Loading ─────────────────────────────────────────────────

    /**
     * Load the dictionary from assets. Call once on app startup.
     * Safe to call multiple times — only loads once.
     *
     * @param context Android context for asset access.
     * @return Number of words loaded, or 0 if already loaded.
     */
    fun load(context: Context): Int {
        if (isLoaded) return 0

        return try {
            val pairs = mutableListOf<Pair<String, Int>>()
            val freqMap = mutableMapOf<String, Int>()

            context.assets.open(DICT_FILE).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && ':' in trimmed) {
                        val lastColon = trimmed.lastIndexOf(':')
                        val word = trimmed.substring(0, lastColon).lowercase()
                        val freq = trimmed.substring(lastColon + 1).toIntOrNull() ?: 0
                        if (word.isNotEmpty()) {
                            pairs.add(word to freq)
                            freqMap[word] = freq
                        }
                    }
                }
            }

            // Sort alphabetically for binary search
            entries = pairs.sortedBy { it.first }
            frequencyMap = freqMap

            Log.i(TAG, "Loaded ${entries.size} words from dictionary")
            entries.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dictionary", e)
            0
        }
    }

    // ─── Prefix Matching ─────────────────────────────────────────

    /**
     * Get word suggestions matching a prefix, ranked by frequency.
     *
     * Uses binary search to find the prefix range, then sorts by frequency.
     *
     * @param prefix The typed prefix (case-insensitive).
     * @param limit Maximum number of suggestions to return.
     * @return List of matching words, sorted by frequency (highest first).
     */
    fun getSuggestions(prefix: String, limit: Int = 5): List<String> {
        if (prefix.isEmpty() || entries.isEmpty()) return emptyList()

        val lowerPrefix = prefix.lowercase()

        // Binary search for first matching entry
        val startIndex = findFirstMatch(lowerPrefix)
        if (startIndex == -1) return emptyList()

        // Collect all matches
        val matches = mutableListOf<Pair<String, Int>>()
        var i = startIndex
        while (i < entries.size) {
            val (word, freq) = entries[i]
            if (!word.startsWith(lowerPrefix)) break
            // Don't suggest the exact word the user already typed
            if (word != lowerPrefix) {
                matches.add(word to freq)
            }
            i++
        }

        // Sort by frequency descending, take top N
        return matches
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Get the frequency score for a word.
     *
     * @param word The word to look up.
     * @return Frequency score, or 0 if not found.
     */
    fun getFrequency(word: String): Int {
        return frequencyMap[word.lowercase()] ?: 0
    }

    /**
     * Check if a word exists in the dictionary.
     *
     * @param word The word to check.
     * @return true if the word is in the dictionary.
     */
    fun contains(word: String): Boolean {
        return frequencyMap.containsKey(word.lowercase())
    }

    // ─── Fuzzy Matching ───────────────────────────────────────────

    /**
     * Get suggestions using fuzzy matching (typo tolerance).
     * Finds words within Levenshtein distance 1-2 of the input.
     *
     * Called when prefix matching returns too few results.
     * Handles common typos: "teh" → "the", "adn" → "and", "wrok" → "work"
     *
     * @param input What the user typed (possibly misspelled).
     * @param limit Maximum suggestions.
     * @return List of close matches, sorted by (edit distance, frequency).
     */
    fun getFuzzySuggestions(input: String, limit: Int = 5): List<String> {
        if (input.isEmpty() || entries.isEmpty()) return emptyList()

        val lower = input.lowercase()
        val maxDistance = if (lower.length <= 3) 1 else 2

        // Only check words within ±2 length of input (optimization)
        val minLen = (lower.length - maxDistance).coerceAtLeast(1)
        val maxLen = lower.length + maxDistance

        val candidates = mutableListOf<Triple<String, Int, Int>>() // word, freq, distance

        for ((word, freq) in entries) {
            if (word.length < minLen || word.length > maxLen) continue
            if (word == lower) continue // exact match excluded

            val dist = levenshteinDistance(lower, word)
            if (dist <= maxDistance) {
                candidates.add(Triple(word, freq, dist))
            }
        }

        // Sort by: edit distance ASC, then frequency DESC
        return candidates
            .sortedWith(compareBy<Triple<String, Int, Int>> { it.third }.thenByDescending { it.second })
            .take(limit)
            .map { it.first }
    }

    /**
     * Levenshtein distance between two strings.
     * Returns the minimum number of single-character edits (insert, delete, replace)
     * needed to transform one string into the other.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Use single-row DP for memory efficiency
        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,      // delete
                    curr[j - 1] + 1,  // insert
                    prev[j - 1] + cost // replace
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }

        return prev[len2]
    }

    // ─── Private ─────────────────────────────────────────────────

    /**
     * Binary search for the first entry matching the prefix.
     *
     * @return Index of first match, or -1 if none found.
     */
    private fun findFirstMatch(prefix: String): Int {
        var low = 0
        var high = entries.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) / 2
            val midWord = entries[mid].first

            when {
                midWord.startsWith(prefix) -> {
                    result = mid
                    high = mid - 1 // Keep searching left for first match
                }
                midWord < prefix -> low = mid + 1
                else -> high = mid - 1
            }
        }

        return result
    }
}
