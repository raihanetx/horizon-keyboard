package com.horizon.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Persistent repository for clipboard history and saved clips.
 *
 * Two lists:
 * - **History**: auto-tracked clipboard entries (max 50), persisted via SharedPreferences
 * - **Saved**: user-bookmarked clips (persistent until manually cleared)
 *
 * Deduplication: checks the ENTIRE history, not just the first item.
 */
class ClipboardRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = mutableListOf<String>()
    val history: List<String> get() = _history.toList()

    private val _saved = mutableListOf<String>()
    val saved: List<String> get() = _saved.toList()

    init {
        loadFromDisk()
    }

    // ─── History Operations ──────────────────────────────────────

    /**
     * Add a new clip to history.
     * Deduplicates against ALL history entries (not just first).
     * If duplicate found, moves it to the top instead of adding again.
     *
     * @return true if the list changed, false if empty/too short.
     */
    fun addToHistory(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length < 2) return false

        // Check for duplicate anywhere in history
        val existingIndex = _history.indexOf(trimmed)
        if (existingIndex == 0) {
            // Already at top — nothing to do
            return false
        }
        if (existingIndex > 0) {
            // Exists but not at top — move to top
            _history.removeAt(existingIndex)
        }

        _history.add(0, trimmed)
        while (_history.size > MAX_HISTORY_SIZE) {
            _history.removeAt(_history.lastIndex)
        }
        saveHistory()
        return true
    }

    fun removeFromHistory(index: Int) {
        if (index in _history.indices) {
            _history.removeAt(index)
            saveHistory()
        }
    }

    fun clearHistory() {
        _history.clear()
        saveHistory()
    }

    // ─── Saved Operations ────────────────────────────────────────

    fun saveClip(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _saved.contains(trimmed)) return false
        _saved.add(0, trimmed)
        saveSaved()
        return true
    }

    fun removeFromSaved(index: Int) {
        if (index in _saved.indices) {
            _saved.removeAt(index)
            saveSaved()
        }
    }

    fun clearSaved() {
        _saved.clear()
        saveSaved()
    }

    fun isSaved(text: String): Boolean = _saved.contains(text.trim())

    // ─── Search ──────────────────────────────────────────────────

    fun searchHistory(query: String): List<String> {
        if (query.isBlank()) return history
        val q = query.lowercase()
        return _history.filter { it.lowercase().contains(q) }
    }

    fun searchSaved(query: String): List<String> {
        if (query.isBlank()) return saved
        val q = query.lowercase()
        return _saved.filter { it.lowercase().contains(q) }
    }

    // ─── Persistence ─────────────────────────────────────────────

    private fun loadFromDisk() {
        _history.clear()
        _history.addAll(loadList(KEY_HISTORY))
        _saved.clear()
        _saved.addAll(loadList(KEY_SAVED))
    }

    private fun saveHistory() = saveList(KEY_HISTORY, _history)
    private fun saveSaved() = saveList(KEY_SAVED, _saved)

    private fun loadList(key: String): List<String> {
        val json = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveList(key: String, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "horizon_clipboard"
        private const val KEY_HISTORY = "clipboard_history"
        private const val KEY_SAVED = "clipboard_saved"
        const val MAX_HISTORY_SIZE = 50
    }
}
