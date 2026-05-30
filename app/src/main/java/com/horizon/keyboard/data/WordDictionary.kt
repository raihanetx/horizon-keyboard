package com.horizon.keyboard.data

object WordDictionary {
    private val words = setOf(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "had",
        "her", "was", "one", "our", "out", "has", "have", "been", "some", "them",
        "than", "what", "when", "with", "about", "after", "also", "back", "because", "come",
        "could", "day", "even", "find", "first", "give", "good", "great", "help", "here",
        "home", "just", "know", "last", "life", "like", "line", "little", "make", "might",
        "more", "most", "much", "must", "need", "never", "next", "now", "only", "open",
        "over", "own", "people", "place", "right", "same", "say", "school", "should", "still",
        "such", "take", "tell", "thing", "think", "time", "under", "until", "very", "want",
        "way", "well", "where", "which", "while", "world", "year", "before", "between", "change",
        "down", "each", "follow", "form", "hand", "high", "large", "learn", "letter", "long",
        "look", "mean", "move", "number", "point", "problem", "public", "result", "run", "second",
        "set", "show", "side", "small", "state", "study", "turn", "use", "water", "word"
    )

    fun getSuggestions(prefix: String, max: Int = 4): List<String> {
        if (prefix.isEmpty()) return emptyList()
        return words
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .sorted()
            .take(max)
    }
}
