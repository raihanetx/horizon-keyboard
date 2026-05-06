package com.horizon.keyboard.core.dictionary

import android.content.Context
import android.util.Log

/**
 * Orchestrates all prediction engines to provide intelligent word suggestions.
 *
 * Combines three layers:
 * 1. **DictionaryEngine** — 10K words with frequency (prefix matching)
 * 2. **UserDictionaryEngine** — learns YOUR words (personal frequency)
 * 3. **BigramEngine** — learns word pairs ("good" → "morning")
 *
 * Scoring formula:
 *   finalScore = (dictFreq × 0.3) + (userFreq × 0.5) + (bigramBonus × 0.2)
 *
 * This mimics how Gboard/SwiftKey rank suggestions — user behavior
 * dominates, context (previous word) boosts, dictionary is baseline.
 */
class SuggestionManager {

    companion object {
        private const val TAG = "SuggestionManager"

        // Scoring weights
        private const val WEIGHT_DICT = 0.3
        private const val WEIGHT_USER = 0.5
        private const val WEIGHT_BIGRAM = 0.2

        // Bigram bonus multiplier (how much a bigram match boosts the score)
        private const val BIGRAM_MULTIPLIER = 100.0

        // Max suggestions to show
        const val MAX_SUGGESTIONS = 5

        // Min word length to learn
        private const val MIN_LEARN_LENGTH = 2
    }

    // ── Engines ──────────────────────────────────────────────────

    private val dictionary = DictionaryEngine()
    private val userDictionary = UserDictionaryEngine()
    private val bigram = BigramEngine()

    /** Whether all engines are loaded and ready. */
    val isReady: Boolean
        get() = dictionary.isLoaded

    // ── Initialization ───────────────────────────────────────────

    private var appContext: Context? = null

    /**
     * Load all engines. Call once on keyboard startup.
     * Thread-safe — loads sequentially, reads are lock-free after.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val dictCount = dictionary.load(context)
        val userCount = userDictionary.load(context)
        val bigramCount = bigram.load(context)
        Log.i(TAG, "Initialized: dict=$dictCount, user=$userCount, bigrams=$bigramCount")
    }

    // ── Suggestion API ───────────────────────────────────────────

    /**
     * Get word suggestions based on current input and context.
     *
     * @param currentInput What the user is currently typing (e.g. "goo")
     * @param previousWord The last completed word (e.g. "I" in "I goo...")
     * @param limit Max suggestions to return.
     * @return Ranked list of suggestions.
     */
    fun getSuggestions(
        currentInput: String,
        previousWord: String? = null,
        limit: Int = MAX_SUGGESTIONS
    ): List<String> {
        if (currentInput.isEmpty() && previousWord.isNullOrEmpty()) {
            return emptyList()
        }

        // If user hasn't typed anything yet, suggest next words after previous word
        if (currentInput.isEmpty() && !previousWord.isNullOrEmpty()) {
            return getNextWordSuggestions(previousWord, limit)
        }

        // If user is typing, do prefix matching with context boost
        if (currentInput.isNotEmpty()) {
            return getPrefixSuggestions(currentInput, previousWord, limit)
        }

        return emptyList()
    }

    /**
     * Called when user completes a word (space, enter, punctuation).
     * Feeds learning into user dictionary and bigram engine.
     *
     * @param word The completed word.
     * @param previousWord The word before it (for bigram learning).
     */
    fun onWordCompleted(word: String, previousWord: String? = null) {
        val clean = word.lowercase().trim()
        if (clean.length < MIN_LEARN_LENGTH) return
        if (clean.all { !it.isLetter() }) return

        // Learn the word
        userDictionary.addWord(clean)

        // Learn the bigram pair
        if (!previousWord.isNullOrEmpty()) {
            val prevClean = previousWord.lowercase().trim()
            if (prevClean.length >= MIN_LEARN_LENGTH && prevClean.any { it.isLetter() }) {
                bigram.addPair(prevClean, clean)
            }
        }

        // Persist periodically (every 10 words)
        pendingSaves++
        if (pendingSaves >= 10) {
            saveAll()
            pendingSaves = 0
        }
    }

    /**
     * Force-save all learned data to disk.
     * Call on keyboard hide/destroy to prevent data loss.
     */
    fun saveAll() {
        // These are no-ops if nothing changed, but safe to call
        userDictionary.save(getAppContext())
        bigram.save(getAppContext())
    }

    /**
     * Clear all user-learned data (reset to factory state).
     */
    fun clearUserData() {
        userDictionary.clear()
        bigram.clear()
        saveAll()
    }

    // ── Private: Core Prediction Logic ───────────────────────────

    /**
     * Prefix-based suggestions with context boosting and fuzzy fallback.
     * This is the main prediction path — called on every keystroke.
     */
    private fun getPrefixSuggestions(
        prefix: String,
        previousWord: String?,
        limit: Int
    ): List<String> {
        val lowerPrefix = prefix.lowercase()

        // Collect candidates from all sources
        val candidates = mutableMapOf<String, Double>()

        // Layer 1: Dictionary prefix matches
        val prefixMatches = dictionary.getSuggestions(lowerPrefix, 20)
        prefixMatches.forEach { word ->
            val dictFreq = dictionary.getFrequency(word).toDouble()
            val normalizedFreq = normalizeFrequency(dictFreq, 100000.0)
            candidates[word] = normalizedFreq * WEIGHT_DICT
        }

        // Layer 2: User dictionary prefix matches (higher weight)
        userDictionary.getSuggestions(lowerPrefix, 10).forEach { word ->
            val userFreq = userDictionary.getFrequency(word).toDouble()
            val normalizedFreq = normalizeFrequency(userFreq, 1000.0)
            val existing = candidates[word] ?: 0.0
            candidates[word] = existing + normalizedFreq * WEIGHT_USER
        }

        // Layer 2.5: Fuzzy matching fallback (when prefix results are few)
        if (candidates.size < 3 && lowerPrefix.length >= 2) {
            dictionary.getFuzzySuggestions(lowerPrefix, 5).forEach { word ->
                if (!candidates.containsKey(word)) {
                    val dictFreq = dictionary.getFrequency(word).toDouble()
                    val normalizedFreq = normalizeFrequency(dictFreq, 100000.0)
                    // Fuzzy matches get a penalty (×0.4) — they're guesses, not exact
                    candidates[word] = normalizedFreq * WEIGHT_DICT * 0.4
                }
            }
        }

        // Layer 3: Bigram boost (if we have context)
        if (!previousWord.isNullOrEmpty()) {
            val prevLower = previousWord.lowercase().trim()
            bigram.getNextWords(prevLower, 10).forEach { nextWord ->
                if (nextWord.startsWith(lowerPrefix)) {
                    val pairCount = bigram.getPairCount(prevLower, nextWord).toDouble()
                    val bigramScore = normalizeFrequency(pairCount, 50.0)
                    val existing = candidates[nextWord] ?: 0.0
                    candidates[nextWord] = existing + bigramScore * WEIGHT_BIGRAM * BIGRAM_MULTIPLIER
                }
            }
        }

        // Exclude the exact prefix (don't suggest what user already typed)
        candidates.remove(lowerPrefix)

        // Sort by final score, take top N
        return candidates.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Next-word suggestions when user hasn't started typing yet.
     * Uses bigram engine primarily.
     */
    private fun getNextWordSuggestions(previousWord: String, limit: Int): List<String> {
        val prevLower = previousWord.lowercase().trim()

        // Bigram predictions (most relevant for next-word)
        val bigramSuggestions = bigram.getNextWords(prevLower, limit)

        if (bigramSuggestions.isNotEmpty()) {
            return bigramSuggestions
        }

        // Fallback: user's most frequent words
        return userDictionary.getTopWords(limit)
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Normalize a frequency value to 0.0 - 1.0 range.
     * Uses log scaling to prevent high-frequency words from dominating.
     */
    private fun normalizeFrequency(freq: Double, maxFreq: Double): Double {
        if (freq <= 0) return 0.0
        return (Math.log1p(freq) / Math.log1p(maxFreq)).coerceIn(0.0, 1.0)
    }

    private var pendingSaves = 0

    /**
     * Get app context for saving. Uses a stored reference.
     * Set during initialize().
     */
    private fun getAppContext(): Context {
        return appContext ?: throw IllegalStateException("SuggestionManager not initialized")
    }
}
