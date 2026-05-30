package com.horizon.keyboard.data.repository

/**
 * Word dictionary for autocomplete suggestions.
 * Contains common English words for demonstration.
 */
object WordRepository {
    
    private val commonWords = setOf(
        // Top 100 most common words
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
    )
    
    private val extendedWords = commonWords + setOf(
        // Additional common words
        "about", "above", "after", "again", "against", "ago", "ahead", "allow", "almost", "alone",
        "already", "always", "among", "another", "answer", "anyone", "anything", "anywhere", "appear", "area",
        "around", "away", "back", "basic", "become", "before", "begin", "behind", "below", "best",
        "better", "between", "both", "bring", "build", "business", "call", "care", "carry", "case",
        "catch", "cause", "center", "chance", "change", "check", "child", "choice", "choose", "city",
        "claim", "clear", "close", "cold", "come", "common", "compare", "complete", "concern", "condition",
        "consider", "continue", "control", "cost", "country", "couple", "course", "create", "cross", "current",
        "dark", "data", "dead", "deal", "death", "deep", "develop", "die", "difference", "difficult",
        "direction", "discover", "discussion", "doctor", "dog", "door", "down", "draw", "dream", "drive",
        "drop", "dry", "during", "each", "early", "earth", "east", "easy", "eat", "edge",
        "effect", "effort", "eight", "either", "else", "end", "enjoy", "enough", "enter", "entire",
        "especially", "event", "every", "everybody", "everyone", "everything", "evidence", "example", "exist", "expect",
        "experience", "explain", "eye", "face", "fact", "fail", "fall", "family", "far", "fast",
        "father", "fear", "feel", "few", "field", "fight", "figure", "fill", "finally", "find",
        "finger", "finish", "fire", "fish", "five", "floor", "fly", "follow", "food", "foot",
        "force", "foreign", "forget", "form", "former", "forward", "four", "free", "friend", "front",
        "full", "fund", "future", "game", "garden", "general", "girl", "glass", "god", "gold",
        "government", "great", "green", "ground", "group", "grow", "growth", "guess", "gun", "guy",
        "hair", "half", "hang", "happen", "happy", "hard", "hate", "head", "health", "hear",
        "heart", "heat", "heavy", "help", "hide", "high", "history", "hit", "hold", "hole",
        "hope", "horse", "hospital", "hot", "hotel", "hour", "house", "huge", "human", "hundred",
        "husband", "idea", "identify", "image", "imagine", "impact", "important", "improve", "include", "increase",
        "indeed", "indicate", "industry", "information", "inside", "instead", "interest", "international", "interview", "involve",
        "issue", "item", "itself", "job", "join", "joy", "judge", "jump", "just", "keep",
        "key", "kid", "kill", "kind", "king", "kitchen", "know", "land", "language", "large",
        "last", "late", "later", "laugh", "law", "leader", "learn", "least", "leave", "left",
        "leg", "less", "letter", "level", "lie", "life", "light", "like", "likely", "line",
        "list", "listen", "little", "live", "local", "long", "look", "lose", "loss", "lost",
        "love", "machine", "main", "major", "manage", "manager", "many", "market", "matter", "may",
        "maybe", "mean", "measure", "media", "medical", "meet", "member", "memory", "mention", "message",
        "method", "middle", "might", "military", "million", "mind", "minute", "miss", "modern", "moment",
        "money", "month", "more", "morning", "most", "mother", "mouth", "move", "movement", "movie",
        "music", "must", "name", "nation", "national", "natural", "nature", "near", "nearly", "necessary",
        "need", "network", "never", "news", "next", "nice", "night", "none", "nor", "north",
        "note", "nothing", "notice", "number", "occur", "off", "offer", "office", "officer", "official",
        "often", "oil", "old", "once", "only", "open", "operation", "opportunity", "option", "order",
        "organization", "others", "outside", "owner", "page", "pain", "painting", "paper", "parent", "part",
        "particular", "partner", "party", "pass", "past", "pattern", "pay", "peace", "people", "per",
        "perform", "performance", "perhaps", "period", "person", "personal", "phone", "physical", "pick", "picture",
        "piece", "place", "plan", "plant", "play", "player", "please", "point", "police", "policy",
        "political", "poor", "popular", "position", "positive", "possible", "power", "practice", "prepare", "present",
        "president", "pressure", "pretty", "prevent", "price", "private", "probably", "problem", "process", "produce",
        "product", "production", "professional", "program", "project", "property", "protect", "prove", "provide", "public",
        "pull", "purpose", "push", "quality", "question", "quickly", "quite", "race", "raise", "range",
        "rate", "rather", "reach", "read", "ready", "real", "reality", "realize", "reason", "receive",
        "recent", "recently", "recognize", "record", "red", "reduce", "reflect", "region", "relate", "relationship",
        "religious", "remain", "remember", "remove", "report", "represent", "require", "research", "resource", "respond",
        "response", "rest", "result", "return", "reveal", "rich", "right", "rise", "risk", "road",
        "rock", "role", "room", "rule", "run", "safe", "save", "scene", "science", "season",
        "seat", "second", "section", "security", "seek", "seem", "sell", "send", "senior", "sense",
        "series", "serious", "serve", "service", "set", "seven", "several", "shake", "share", "she",
        "shoot", "short", "shot", "should", "shoulder", "show", "side", "sign", "significant", "similar",
        "simple", "simply", "since", "sing", "single", "sister", "sit", "site", "situation", "six",
        "size", "skill", "skin", "small", "smile", "social", "society", "soldier", "some", "somebody",
        "someone", "something", "sometimes", "son", "song", "soon", "sort", "sound", "source", "south",
        "southern", "space", "speak", "special", "specific", "speech", "spend", "sport", "spring", "staff",
        "stage", "stand", "standard", "star", "start", "state", "statement", "station", "stay", "step",
        "still", "stop", "story", "strategy", "street", "strong", "structure", "student", "study", "stuff",
        "style", "subject", "success", "successful", "such", "suddenly", "suffer", "suggest", "summer", "support",
        "sure", "surface", "system", "table", "take", "talk", "task", "tax", "teach", "teacher",
        "team", "technology", "television", "tell", "ten", "tend", "term", "test", "than", "thank",
        "that", "the", "their", "them", "themselves", "then", "there", "these", "they", "thing",
        "think", "third", "those", "though", "thought", "thousand", "threat", "three", "through", "throughout",
        "throw", "thus", "today", "together", "tonight", "too", "total", "tough", "toward", "town",
        "trade", "traditional", "training", "travel", "treat", "treatment", "tree", "trial", "trip", "trouble",
        "true", "truth", "try", "turn", "TV", "two", "type", "under", "understand", "unit",
        "until", "upon", "usually", "value", "various", "very", "victim", "view", "violence", "visit",
        "voice", "vote", "wait", "walk", "wall", "want", "war", "watch", "water", "weapon",
        "wear", "week", "weight", "west", "western", "whatever", "whether", "while", "white", "whole",
        "whom", "whose", "why", "wide", "wife", "win", "wind", "window", "wish", "with",
        "within", "without", "woman", "wonder", "word", "work", "worker", "world", "worry", "would",
        "write", "writer", "wrong", "yard", "yeah", "year", "yes", "yet", "young", "your",
        "yourself", "youth"
    )
    
    /**
     * Get word suggestions based on prefix.
     */
    fun getSuggestions(prefix: String, maxResults: Int = 5): List<String> {
        if (prefix.isBlank()) return emptyList()
        
        val lowerPrefix = prefix.lowercase()
        
        return extendedWords
            .filter { it.startsWith(lowerPrefix) }
            .sorted()
            .take(maxResults)
    }
}
