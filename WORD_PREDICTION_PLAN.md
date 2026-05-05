# Horizon Keyboard — Word Prediction System

**Goal:** Replace the static 5-word suggestion bar with a professional word prediction engine that learns from user typing.

**Based on:** Professional keyboard architecture (dictionary + user learning + bigram prediction).

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│              SuggestionManager                   │
│         (orchestrates all engines)               │
├─────────────┬──────────────┬────────────────────┤
│ Dictionary  │ User Dict    │ Bigram Engine      │
│ Engine      │ Engine       │                    │
├─────────────┼──────────────┼────────────────────┤
│ 10K words   │ Learns YOUR  │ Learns word pairs  │
│ + frequency │ words        │ "good" → "morning" │
│ Prefix match│ Persists     │ Persists           │
│ "hel"→"hello│ to JSON      │ to JSON            │
├─────────────┴──────────────┴────────────────────┤
│              Storage Layer                       │
│  assets/dictionaries/en_frequency.txt            │
│  app_data/user_dictionary.json                   │
│  app_data/user_bigrams.json                      │
└─────────────────────────────────────────────────┘
```

---

## Issues Found

| # | Issue | Impact |
|---|-------|--------|
| 1 | No dictionary engine | Can't suggest words while typing |
| 2 | No user learning | Keyboard doesn't improve over time |
| 3 | No bigram prediction | No next-word suggestions |
| 4 | Static suggestion bar | Shows "I, Hello, The, Thanks, How" forever |
| 5 | No word capture from typing | Can't learn from user input |

---

## Execution Plan — 6 Steps

### FIX 1 — Create Dictionary Data File

**Problem:** No word frequency data exists.

**Fix:** Create `assets/dictionaries/en_frequency.txt` with top 5,000 English words and frequency scores. Format: `word:frequency` per line.

```
NEW: app/src/main/assets/dictionaries/en_frequency.txt
```

**Data source:** Public domain English word frequency (derived from Wiktionary/Common Crawl). No legal issues.

**Verify:** File exists, has 5000+ entries, format is correct.

---

### FIX 2 — Create DictionaryEngine

**Problem:** No engine to load and query the dictionary.

**Fix:** Create `core/dictionary/DictionaryEngine.kt` — loads the frequency file, provides prefix matching with ranking.

```
NEW: app/src/main/java/com/horizon/keyboard/core/dictionary/DictionaryEngine.kt
```

**API:**
- `load(context)` — reads file into memory
- `getSuggestions(prefix: String, limit: Int): List<String>` — returns top matches
- `getFrequency(word: String): Int` — returns frequency score

**Verify:** Unit test — `getSuggestions("hel", 3)` returns "hello", "help", "hell".

---

### FIX 3 — Create UserDictionaryEngine

**Problem:** Keyboard doesn't learn user's words.

**Fix:** Create `core/dictionary/UserDictionaryEngine.kt` — stores words the user types, increases frequency on repeat use, persists to JSON.

```
NEW: app/src/main/java/com/horizon/keyboard/core/dictionary/UserDictionaryEngine.kt
```

**API:**
- `load(context)` — reads from `user_dictionary.json`
- `addWord(word: String)` — adds word or increments frequency
- `getSuggestions(prefix: String, limit: Int): List<String>` — returns user's words matching prefix
- `save()` — persists to JSON

**Storage:** `context.filesDir/user_dictionary.json`

**Verify:** Unit test — add "raihan" 3 times, verify frequency is 3.

---

### FIX 4 — Create BigramEngine

**Problem:** No next-word prediction.

**Fix:** Create `core/dictionary/BigramEngine.kt` — tracks word pairs, suggests next word based on previous word.

```
NEW: app/src/main/java/com/horizon/keyboard/core/dictionary/BigramEngine.kt
```

**API:**
- `load(context)` — reads from `user_bigrams.json`
- `addPair(word1: String, word2: String)` — records a word pair
- `getNextWords(word: String, limit: Int): List<String>` — suggests next words
- `save()` — persists to JSON

**Storage:** `context.filesDir/user_bigrams.json`

**Verify:** Unit test — addPair("good", "morning") 5 times, getNextWords("good") returns ["morning"].

---

### FIX 5 — Create SuggestionManager + Welcome Message

**Problem:** No orchestrator, no welcome message.

**Fix:** Create `core/dictionary/SuggestionManager.kt` — combines all three engines, handles welcome state, merges and ranks suggestions.

```
NEW: app/src/main/java/com/horizon/keyboard/core/dictionary/SuggestionManager.kt
```

**API:**
- `initialize(context)` — loads all engines
- `getSuggestions(currentInput: String, previousWord: String?): List<String>` — merged suggestions
- `onWordCompleted(word: String, previousWord: String?)` — triggers learning
- `getWelcomeMessage(): String` — returns "Welcome to Horizon Keyboard 👋"

**Logic:**
1. If no input → show bigram suggestions for previous word, else show welcome
2. If typing → prefix match from dictionary + user dict, merge by score
3. On word completion → add to user dict + record bigram pair

**Verify:** Unit test — suggestions merge correctly, welcome shows when idle.

---

### FIX 6 — Update SuggestionBar + Wire into KeyboardView

**Problem:** SuggestionBar shows static words, KeyboardView doesn't feed words to engines.

**Fix:**
- Update `SuggestionBar` to accept dynamic word list from `SuggestionManager`
- Update `KeyboardView` to call `SuggestionManager` on every keypress
- Update `KeyboardView` to call `onWordCompleted` when space/enter is pressed
- Show welcome message when keyboard first appears

```
EDIT: ui/bar/SuggestionBar.kt — accept dynamic suggestions list
EDIT: KeyboardView.kt — wire SuggestionManager into key handling
```

**Verify:** Type "hel" → see "hello", "help", "hell" in suggestion bar. Type "good morning" multiple times → "morning" appears after "good".

---

## Execution Order

```
FIX 1 ─→ FIX 2 ─→ FIX 3 ─→ FIX 4 ─→ FIX 5 ─→ FIX 6
  │         │         │         │         │         │
  v         v         v         v         v         v
  data    dict      user     bigram   manager   wire
  file    engine    dict     engine   + welcome  into UI
```

Each fix builds on the previous one. Dependencies flow top-down.

---

## Success Criteria

- [ ] Dictionary loads 5000+ words on startup
- [ ] Typing "hel" shows "hello" as first suggestion
- [ ] User's custom words appear in suggestions after typing them
- [ ] Bigram predictions appear after completing a word
- [ ] Welcome message shows when keyboard first appears
- [ ] All engines persist data to JSON (survives app restart)
- [ ] Unit tests pass for all engines
- [ ] All changes committed and pushed to GitHub

---

## Estimated Effort

| Fix | Complexity | Risk | Time |
|-----|-----------|------|------|
| FIX 1 | Low | None | 5 min |
| FIX 2 | Medium | Low | 10 min |
| FIX 3 | Medium | Low | 10 min |
| FIX 4 | Medium | Low | 10 min |
| FIX 5 | Medium | Medium | 10 min |
| FIX 6 | Medium | Medium | 10 min |
| **Total** | | | **~55 min** |
