# Horizon Keyboard — Build & Approach

## Quick Build

```bash
# Clean + build
rm -rf app/build
export JAVA_HOME=/nix/store/lrxr7zmja7rczsp11bll9rsli5hnmh6c-openjdk-17.0.7+7/lib/openjdk
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug --no-daemon

# APK output:
#   app/build/outputs/apk/debug/app-debug.apk
```

## Environment

| Thing | Path |
|---|---|
| Project root | `/home/user/horizon-keyboard` |
| Java | openjdk-17.0.7+7 (installed via nix) |
| Android SDK | `/home/user/android-sdk` |
| Gradle | 8.2 (wrapper auto-downloads) |
| Kotlin | 1.9.x (via compose BOM 2023.10.01) |

### Key files

- `local.properties` — points `sdk.dir=/home/user/android-sdk`
- `app/build.gradle.kts` — dependencies: Compose, Material3, Material Icons Extended, ViewModel
- `gradle.properties` — 2GB heap, AndroidX enabled

### Environment variables needed

```bash
export JAVA_HOME=/nix/store/lrxr7zmja7rczsp11bll9rsli5hnmh6c-openjdk-17.0.7+7/lib/openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

The SDK location is already in `local.properties`, so no `ANDROID_HOME` needed.

---

## Project Structure

```
app/src/main/java/com/horizon/keyboard/
├── data/
│   ├── KeyType.kt              # Enum: LETTER, SHIFT, BACKSPACE, SPACE, etc.
│   ├── KeyboardLayout.kt       # KeyItem + KeyboardRow data classes + layouts
│   └── WordDictionary.kt       # 100 common words, prefix matching
├── ui/
│   ├── components/
│   │   ├── KeyButton.kt        # Single key rendering (border-bottom, press anim)
│   │   ├── KeyboardRow.kt      # Row with 6dp gap, fixed-width keys
│   │   ├── KeyboardView.kt     # Full keyboard: toolbar + key rows
│   │   └── ToolbarView.kt      # 3 modes: Default / Typing (predictions) / Voice
│   └── theme/
│       ├── Color.kt            # Black/gray monochrome palette
│       └── Theme.kt            # Material3 light/dark color schemes
├── viewmodel/
│   └── KeyboardViewModel.kt    # Shift, numbers layout, suggestions, key handling
└── MainActivity.kt             # TextField + keyboard + state management
```

---

## Approach

### 1. Reference-first

The file `new ui.html` was the visual reference. Every measurement came from it:
- Key heights (40px), widths (32dp / 34.14dp / 35.14dp / 40dp / 48dp / 174dp)
- Gaps (keys: 6dp, rows: 8dp)
- Padding (container: 8dp sides, 8dp top, 32dp bottom)
- Toolbar height: 36dp
- Viewport reference: 390px width

### 2. Pixel-perfect data layout

Instead of `weight(1f)` for letter keys (which stretches unevenly on different widths), every key has an exact `widthDp` stored in `KeyItem`. This matches the 390px reference exactly.

### 3. State in ViewModel + MainActivity

- `KeyboardViewModel` holds keyboard state: shift, numbers mode, key press callbacks
- `Text content` + `toolbar mode` live in `MainActivity` (the screen owner)
- Suggestions are computed as a pure function `getSuggestions(text)` — no side effects

### 4. Toolbar has 3 modes

- **Default** — all 6 icons (keyboard, emoji, mic, clipboard, translate, settings)
- **Typing** — auto-activates when text is non-empty: `[←] [suggestions] [🔊]`
- **Voice** — when mic/volume tapped: `[EN/BN] [Listening...] [■]`

### 5. Visual decisions

- No shadows or 3D effects (flat design)
- Bottom-border only on keys (not full outline)
- Top-border only on keyboard container
- Suggestion chips: plain text with `·` separators, no background
- Black/gray monochrome scheme

### 6. Build speed tricks

- `--no-daemon` prevents daemon lock issues in constrained envs
- `rm -rf app/build` ensures clean state
- Gradle properties set 2GB heap (`-Xmx2048m`)
- Fresh build ~2 minutes on this hardware

---

## Iteration history

1. Initial conversion from HTML to Compose
2. Removed shadows / blue tones → monochrome
3. Added numbers/symbols layout (123 button)
4. Added word prediction (WordDictionary + ViewModel logic)
5. Fixed suggestion sync bug (dual state → single source of truth)
6. Moved suggestions to toolbar header (3 modes design)
7. Added voice typing toolbar UI
8. Pinned exact key widths to reference spec

---

## Key design rules

- `KeyItem.widthDp` — always use exact dp, never `weight(1f)` for keys
- One source of truth for text (`textContent` in MainActivity)
- Suggestions are pure functions of text, not stored state
- Toolbar mode is explicit: DEFAULT → TYPING → VOICE, no overlapping states
