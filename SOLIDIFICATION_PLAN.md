# Horizon Keyboard — Solidification Plan

**Goal:** Turn the refactored codebase from 7/10 → 9/10 professional grade.

**Based on:** Senior developer audit (2026-05-05)

---

## Issues Found (Priority Order)

### 🔴 Critical (Must Fix)

| # | Issue | Impact | Files Affected |
|---|---|---|---|
| 1 | `VoiceEngineRouter` imports `SettingsPanel` (UI) | Dependency leak — voice layer depends on UI | `VoiceEngineRouter.kt`, `SettingsPanel.kt` |
| 2 | `KeyboardMode` sealed class exists but is never used | Dead code, state machine wasted | `KeyboardMode.kt`, `PanelHost.kt`, `KeyboardView.kt` |
| 3 | `IconView.kt` — imported by 0 files | Dead code, 111 lines wasted | `IconView.kt` |
| 4 | 10 silent catch blocks in critical paths | `SettingsPanel` silently swallows credential failures | `SettingsPanel.kt` |

### 🟡 Important (Should Fix)

| # | Issue | Impact | Files Affected |
|---|---|---|---|
| 5 | 135 references to `KeyboardTheme` shim | Unnecessary indirection layer | 5 files |
| 6 | 4 references to `SecureKeyStore` shim | Unnecessary indirection layer | 2 files |
| 7 | `VoiceLanguage.kt` in root package | Should be in `voice/` or `core/` | `VoiceLanguage.kt` |
| 8 | Compose dead code (1,050 lines) ships in APK | Bloats APK, increases build time | `ui/compose/` |

### 🟢 Nice to Have (Can Defer)

| # | Issue | Impact | Files Affected |
|---|---|---|---|
| 9 | Zero unit tests | No safety net for refactoring | New test files |
| 10 | `KeyViewFactory.kt` at 401 lines | Largest file, could split `EnterKeyAppearance` | `KeyViewFactory.kt` |

---

## Execution Plan — 8 Steps

### FIX 1 — Move `VoiceEngineType` Enum to `core/`

**Problem:** `VoiceEngineRouter` (voice/) imports `SettingsPanel` (ui/) for the `VoiceEngineType` enum.
**Fix:** Extract enum to `core/VoiceEngineType.kt`, update all imports.

```
NEW:  core/VoiceEngineType.kt (enum with 4 values)
EDIT: voice/VoiceEngineRouter.kt — import from core/ instead of ui/
EDIT: ui/panel/SettingsPanel.kt — import from core/ instead of defining inline
EDIT: KeyboardVoiceManager.kt — import from core/
```

**Verify:** `grep -r "SettingsPanel.VoiceEngineType"` returns 0 results.

---

### FIX 2 — Wire Up `KeyboardMode` State Machine

**Problem:** `KeyboardMode` sealed class exists but nobody uses it. Keyboard still uses visibility flags.
**Fix:** Make `PanelHost` track and expose `currentMode`. `KeyboardView` reads mode instead of checking visibility.

```
EDIT: ui/panel/PanelHost.kt — add `var currentMode: KeyboardMode`, update on every switch
EDIT: KeyboardView.kt — read `panelHost.currentMode` for state checks
EDIT: core/KeyboardMode.kt — add `Voice` sealed subtypes if needed
```

**Verify:** No `?.visibility == View.VISIBLE` checks for state logic in KeyboardView.

---

### FIX 3 — Delete Dead `IconView.kt`

**Problem:** Canvas-drawn icon view, imported by 0 files. Keyboard uses vector drawables.
**Fix:** Delete the file.

```
DELETE: ui/theme/IconView.kt
```

**Verify:** `grep -rn "IconView"` returns 0 results outside the deleted file.

---

### FIX 4 — Fix Silent Catches in Settings

**Problem:** `SettingsPanel.loadSettings()` and `saveSettings()` silently swallow credential failures. User thinks key is saved but it isn't.
**Fix:** Log errors, show user-visible feedback.

```
EDIT: ui/panel/SettingsPanel.kt — in loadSettings(), log error + set a flag
EDIT: ui/panel/SettingsPanel.kt — in saveSettings(), return Boolean success/failure
```

**Verify:** No `catch (_: Exception) {}` in settings persistence paths.

---

### FIX 5 — Update All `KeyboardTheme` References to Direct Imports

**Problem:** 135 references go through a shim that delegates to `Colors`/`Drawables`/`Dimensions`.
**Fix:** Update all files to import directly, then delete the shim.

```
EDIT: KeyboardView.kt — replace KeyboardTheme.* with Colors.*/Drawables.*/Dimensions.*
EDIT: KeyboardVoiceManager.kt — same
EDIT: ui/panel/SettingsPanel.kt — same
EDIT: ui/panel/ClipboardPanel.kt — same
EDIT: ui/bar/HeaderBar.kt — same
EDIT: ui/bar/SuggestionBar.kt — same
EDIT: ui/bar/VoiceBar.kt — same
EDIT: ui/keyboard/KeyViewFactory.kt — same
EDIT: ui/keyboard/KeyPopupManager.kt — same
DELETE: KeyboardTheme.kt (shim)
```

**Verify:** `grep -rn "KeyboardTheme\."` returns 0 results.

---

### FIX 6 — Update All `SecureKeyStore` References to Direct Imports

**Problem:** 4 references go through a shim.
**Fix:** Update imports, delete shim.

```
EDIT: ui/panel/SettingsPanel.kt — import data.SecureKeyStore directly
EDIT: VoiceTranscriptionEngine.kt — if referenced
DELETE: SecureKeyStore.kt (root shim)
```

**Verify:** `grep -rn "import com.horizon.keyboard.SecureKeyStore"` returns 0 results.

---

### FIX 7 — Move `VoiceLanguage.kt` to `voice/`

**Problem:** 20-line enum sitting alone in root package.
**Fix:** Move to `voice/`, update all imports.

```
MOVE: VoiceLanguage.kt → voice/VoiceLanguage.kt
EDIT: all files importing VoiceLanguage (SettingsPanel, VoiceEngineRouter, KeyboardVoiceManager, etc.)
```

**Verify:** No file in root package imports `VoiceLanguage` from old location.

---

### FIX 8 — Exclude Compose Dead Code from Release Build

**Problem:** 1,050 lines of experimental Compose code ships in the APK.
**Fix:** Move to a build variant or exclude via ProGuard/build config.

```
OPTION A: Move ui/compose/ to a separate source set (debug only)
OPTION B: Add @Suppress + ProGuard keep rules to strip in release
OPTION C: Delete entirely (if not needed)
```

**Recommendation:** Option A — keep as debug-only reference, exclude from release.

---

## Execution Order

```
FIX 1 ─→ FIX 2 ─→ FIX 3 ─→ FIX 4 ─→ FIX 5 ─→ FIX 6 ─→ FIX 7 ─→ FIX 8
  │         │         │         │         │         │         │         │
  v         v         v         v         v         v         v         v
enum     state    delete   error    theme    keystore  language  compose
extract  machine  dead     handling shim     shim      location  exclusion
```

**FIX 1-4** are critical — fix architectural issues first.
**FIX 5-7** are cleanup — remove unnecessary indirection.
**FIX 8** is build optimization — can be done last.

---

## Success Criteria

- [ ] Zero cross-package dependency violations (voice/ → ui/ = 0)
- [ ] `KeyboardMode` state machine actively used by `PanelHost`
- [ ] Zero dead code files (IconView deleted)
- [ ] Zero dangerous silent catches in persistence paths
- [ ] Zero `KeyboardTheme.*` references (shim deleted)
- [ ] Zero `SecureKeyStore` shim references (shim deleted)
- [ ] `VoiceLanguage` in `voice/` package
- [ ] Compose code excluded from release build
- [ ] All changes pushed to GitHub

---

## Estimated Effort

| Fix | Complexity | Risk | Time |
|---|---|---|---|
| FIX 1 | Low | Low | 5 min |
| FIX 2 | Medium | Medium | 10 min |
| FIX 3 | Trivial | None | 1 min |
| FIX 4 | Low | Low | 5 min |
| FIX 5 | Medium | Low | 10 min |
| FIX 6 | Low | Low | 3 min |
| FIX 7 | Low | Low | 3 min |
| FIX 8 | Medium | Medium | 10 min |
| **Total** | | | **~47 min** |
