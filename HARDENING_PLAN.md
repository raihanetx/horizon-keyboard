# Horizon Keyboard — Hardening Plan

**Goal:** Take the solidified codebase (9/10) → production-ready professional grade.

**Based on:** Full project audit (2026-05-05), post-solidification review.

**Prerequisite:** SOLIDIFICATION_PLAN.md (all 8 fixes completed ✅)

---

## Issues Found (Priority Order)

### 🔴 Critical (Must Fix)

| # | Issue | Impact | Files Affected |
|---|-------|--------|----------------|
| 1 | `proguard-rules.pro` missing — referenced in build.gradle.kts | **Release build will fail** | `proguard-rules.pro` (new) |
| 2 | No LICENSE file | Legally ambiguous — not truly "open source" | `LICENSE` (new) |

### 🟡 Important (Should Fix)

| # | Issue | Impact | Files Affected |
|---|-------|--------|----------------|
| 3 | 5 silent catch blocks in non-settings paths | Errors swallowed in popup, voice session, audio | `KeyPopupManager.kt`, `VoiceSessionManager.kt`, `AudioRecorder.kt` |
| 4 | Zero tests | No safety net for future changes | New test files |

### 🟢 Nice to Have (Can Defer)

| # | Issue | Impact | Files Affected |
|---|-------|--------|----------------|
| 5 | No CI/CD pipeline | Manual builds only | `.github/workflows/` (new) |
| 6 | Root package clutter | `KeyboardView.kt` and `KeyboardVoiceManager.kt` don't belong in root | 2 files + import updates |

---

## Execution Plan — 4 Steps

### FIX 1 — Create `proguard-rules.pro`

**Problem:** `build.gradle.kts` line 24 references `proguard-rules.pro` but the file doesn't exist. Running `./gradlew assembleRelease` will fail.

**Fix:** Create ProGuard rules file with standard Android keyboard app rules.

```
NEW: proguard-rules.pro
```

**Rules to include:**
- Keep `HorizonKeyboardService` (IME entry point)
- Keep `MainActivity` (launcher)
- Keep all classes accessed via `findViewById` or Compose
- Keep `SecureKeyStore` (reflection via EncryptedSharedPreferences)
- Keep `VoiceCommandProcessor` actions (sealed class)
- Standard Android library rules
- Suppress warnings for missing annotations

**Verify:** `./gradlew assembleRelease` compiles without errors.

---

### FIX 2 — Add MIT LICENSE file

**Problem:** README says "Open source. Use it however you like." but no actual license file exists. Without a license, the code is technically "all rights reserved" despite the README claim.

**Fix:** Add MIT License (most permissive, matches the README intent).

```
NEW: LICENSE
```

**Verify:** `LICENSE` file exists with correct year and copyright holder.

---

### FIX 3 — Fix remaining 5 silent catch blocks

**Problem:** Five `catch (_: Exception) {}` blocks outside of settings — errors silently swallowed in UI popups, voice session lifecycle, and audio recording.

**Affected locations:**

| File | Line | Context |
|------|------|---------|
| `KeyPopupManager.kt:76` | `show()` | Popup positioning fails silently |
| `KeyPopupManager.kt:86` | `hide()` | Popup cleanup fails silently |
| `VoiceSessionManager.kt:77` | `stop()` | SpeechRecognizer stop fails silently |
| `VoiceSessionManager.kt:85` | `destroy()` | SpeechRecognizer destroy fails silently |
| `AudioRecorder.kt:104` | `release()` | AudioRecord release fails silently |

**Fix:** Add `Log.w()` with tag and exception details. Keep `try/catch` (these are cleanup operations where we don't want to crash), but log the error.

```
EDIT: KeyPopupManager.kt — add Log.w in show() and hide() catches
EDIT: VoiceSessionManager.kt — add Log.w in stop() and destroy() catches
EDIT: AudioRecorder.kt — add Log.w in release() catch
```

**Verify:** `grep -rn "catch.*Exception.*{}"` returns 0 results.

---

### FIX 4 — Add basic unit tests

**Problem:** Zero tests. Any future refactor is a coin flip.

**Fix:** Add unit tests for the pure-logic components that don't need Android context:

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `VoiceCommandProcessorTest.kt` | Voice commands → actions mapping | All 10 command types |
| `VoiceLanguageTest.kt` | Language enum + fromName lookup | All entries + edge cases |
| `KeyboardModeTest.kt` | Sealed class identity | All 5 modes |
| `DimensionsTest.kt` | maskKey logic | Short keys, long keys, edge cases |

```
NEW: app/src/test/java/com/horizon/keyboard/voice/VoiceCommandProcessorTest.kt
NEW: app/src/test/java/com/horizon/keyboard/voice/VoiceLanguageTest.kt
NEW: app/src/test/java/com/horizon/keyboard/core/KeyboardModeTest.kt
NEW: app/src/test/java/com/horizon/keyboard/ui/theme/DimensionsTest.kt
```

**Verify:** `./gradlew test` passes all tests.

---

## Execution Order

```
FIX 1 ─→ FIX 2 ─→ FIX 3 ─→ FIX 4
  │         │         │         │
  v         v         v         v
proguard  license   silent    unit
rules     file      catches   tests
```

FIX 1-2 are quick wins that fix broken/missing fundamentals.
FIX 3 is cleanup for code quality.
FIX 4 is the safety net for future changes.

---

## Success Criteria

- [ ] `./gradlew assembleRelease` succeeds without errors
- [ ] `LICENSE` file exists with MIT license
- [ ] `grep -rn "catch.*Exception.*{}"` returns 0 results across entire codebase
- [ ] `./gradlew test` passes all unit tests
- [ ] All changes committed and pushed to GitHub

---

## Estimated Effort

| Fix | Complexity | Risk | Time |
|-----|-----------|------|------|
| FIX 1 | Low | Low | 5 min |
| FIX 2 | Trivial | None | 2 min |
| FIX 3 | Low | Low | 5 min |
| FIX 4 | Medium | Low | 15 min |
| **Total** | | | **~27 min** |

---

## What's NOT in This Plan (Intentionally Deferred)

| Item | Reason |
|------|--------|
| CI/CD (GitHub Actions) | Needs repo permissions setup, can be added anytime |
| Root package restructure | Low impact, high risk of merge conflicts |
| Compose dead code cleanup | Already moved to debug source set |
| ProGuard optimization | Basic rules first, optimize later |
