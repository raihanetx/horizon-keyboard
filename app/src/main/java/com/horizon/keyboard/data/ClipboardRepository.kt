package com.horizon.keyboard.data

/**
 * In-memory repository for clipboard history and saved clips.
 *
 * Manages two lists:
 * - **History**: automatically tracked clipboard entries (max 30)
 * - **Saved**: user-bookmarked clips (persistent until cleared)
 *
 * No direct Android dependencies — pure data management.
 * The UI layer reads from here and calls methods to mutate state.
 */
class ClipboardRepository {

    /** Most recent clipboard entries (newest first). Max [MAX_HISTORY_SIZE]. */
    private val _history = mutableListOf<String>()
    val history: List<String> get() = _history.toList()

    /** User-saved clips (newest first). */
    private val _saved = mutableListOf<String>()
    val saved: List<String> get() = _saved.toList()

    // ─── History Operations ──────────────────────────────────────

    /**
     * Add a new clip to the history (deduplicates, newest first).
     * @return true if the clip was added, false if it was a duplicate or empty.
     */
    fun addToHistory(text: String): Boolean {
        if (text.isEmpty()) return false
        if (_history.firstOrNull() == text) return false

        _history.add(0, text)
        if (_history.size > MAX_HISTORY_SIZE) {
            _history.removeAt(_history.lastIndex)
        }
        return true
    }

    /** Remove a clip from history by index. */
    fun removeFromHistory(index: Int) {
        if (index in _history.indices) _history.removeAt(index)
    }

    /** Clear all history clips. */
    fun clearHistory() {
        _history.clear()
    }

    // ─── Saved Operations ────────────────────────────────────────

    /**
     * Save a clip from history to the saved list (deduplicates).
     * @return true if saved, false if already saved or empty.
     */
    fun saveClip(text: String): Boolean {
        if (text.isEmpty() || _saved.contains(text)) return false
        _saved.add(0, text)
        return true
    }

    /** Remove a saved clip by index. */
    fun removeFromSaved(index: Int) {
        if (index in _saved.indices) _saved.removeAt(index)
    }

    /** Clear all saved clips. */
    fun clearSaved() {
        _saved.clear()
    }

    /** Check if a clip is already saved. */
    fun isSaved(text: String): Boolean = _saved.contains(text)

    // ─── State ───────────────────────────────────────────────────

    /** Whether there are any clips at all. */
    val isEmpty: Boolean get() = _history.isEmpty() && _saved.isEmpty()

    companion object {
        const val MAX_HISTORY_SIZE = 30
    }
}
