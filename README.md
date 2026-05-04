# Horizon Keyboard

A minimal Android keyboard built with Jetpack Compose, featuring **dual-engine voice typing** with support for **English** and **15+ Indian languages**.

## Features

### Keyboard
- **QWERTY layout** with number row (1-0)
- **Shift** toggle for uppercase letters
- **Symbol/Number mode** (123 → symbol grid)
- **Contextual enter key** — icon changes based on app (search/send/done/go)
- **Professional Material Design icons** — vector drawables throughout
- Dark theme (iOS-style dark keyboard aesthetic)

### Voice Typing 🎤
- **Dual-engine architecture** — automatic model selection per language
- **Whisper (via Groq)** — English specialist, ~4.4% WER, 2,000 free requests/day
- **Gemma 4** — Bangla specialist, powered by Google's Universal Speech Model
- **Android Built-in** — offline fallback using Google Speech Services
- **Auto mode** — routes English → Whisper, Bangla → Gemma automatically
- Continuous listening with auto-restart on timeout
- Voice commands: "command down", "slash agent", "enter", "backspace", etc.

### Clipboard Manager
- Tracks clipboard history (up to 30 clips)
- Save clips with long press
- Paste/delete with icon buttons
- Saved clips section with star indicators

### Settings Panel
- **Voice engine selector** (Auto / Whisper / Gemma / Android)
- **Language picker** — English or Bangla
- **API key management** — Groq (Whisper) and Google AI Studio (Gemma)
- **Model selection** — Gemma 4 E4B (better) or E2B (faster)

## Voice Engine Architecture

```
┌─────────────────────────────────────────────┐
│              Voice Engine Router             │
├─────────────┬───────────────┬───────────────┤
│  Whisper    │   Gemma 4     │   Android     │
│  (Groq)     │   (Google AI) │   Built-in    │
├─────────────┼───────────────┼───────────────┤
│ English     │ Bangla        │ Offline       │
│ ~4.4% WER   │ USM encoder   │ Variable      │
│ 2000 RPD    │ ~1500 RPD     │ Unlimited     │
│ Verbatim    │ Smart parse   │ Basic         │
└─────────────┴───────────────┴───────────────┘
```

### Auto Mode Logic
1. User selects language in Settings (English or Bangla)
2. Voice bar shows language toggle ("EN" ↔ "BN")
3. When recording stops:
   - If language = Bangla → sends to **Gemma 4 API**
   - If language = English → sends to **Groq Whisper API**
   - If no API key → falls back to **Android built-in**

## Security: NDK Key Storage

API keys are stored as **XOR-obfuscated byte arrays in C++ native code** (.so binary). This is significantly harder to extract than SharedPreferences or decompiled Java.

### How to embed keys:

```bash
# 1. Obfuscate your API key
./obfuscate_key.sh "gsk_your_groq_api_key_here"

# Output:
# static const unsigned char KEY_OBFUSCATED[] = { 0x1D, 0x0A, ... };

# 2. Paste the bytes into app/src/main/cpp/native_keys.cpp

# 3. Rebuild the APK
./gradlew assembleRelease
```

### Key priority:
1. Native-embedded key (from .so binary)
2. User-entered key (from Settings)
3. Empty (engine disabled)

## Architecture

```
com.horizon.keyboard/
├── MainActivity.kt          # Setup screen + permission handling
├── HorizonKeyboardService.kt # InputMethodService (IME lifecycle)
├── KeyboardView.kt          # Main keyboard (View-based, production)
├── VoiceLanguage.kt         # Supported languages enum
├── NativeKeys.kt            # JNI bridge for secure key storage
├── ComposeKeyboardView.kt   # Compose wrapper (alternative)
├── KeyboardScreen.kt        # Compose keyboard UI
└── VoiceTypingBar.kt        # Compose voice bar

app/src/main/cpp/
├── CMakeLists.txt           # NDK build config
└── native_keys.cpp          # Secure API key storage (XOR obfuscated)
```

## Build

```bash
# Clone
git clone https://github.com/raihanetx/horizon-keyboard.git
cd horizon-keyboard

# Set up Android SDK (needs SDK 34 + build-tools 34.0.0)
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# (Optional) Embed API keys securely
./obfuscate_key.sh "your_groq_key"  # paste output into native_keys.cpp
./obfuscate_key.sh "your_gemma_key" # paste output into native_keys.cpp

# Build debug APK
./gradlew assembleDebug

# Output
# app/build/outputs/apk/debug/app-debug.apk
```

## Installation

1. Download `app-debug.apk` from this repo
2. Transfer to your Android device
3. Install (enable "Unknown sources" if needed)
4. Open **Horizon Keyboard** app
5. Tap **Step 1** → Enable "Horizon Keyboard" in system settings
6. Tap **Step 2** → Select "Horizon Keyboard" as input method
7. Grant microphone permission when prompted

## Voice Typing Usage

1. Open any app with a text field
2. Tap the **mic** icon on the keyboard
3. The voice bar appears with language toggle and mic button
4. Tap the **mic** button → speak
5. Tap the **globe** button to switch between English and your Indian language
6. Recognized text is automatically inserted
7. Tap the **close** button to hide the voice bar

## Permissions

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Required for voice typing / speech recognition |
| `BIND_INPUT_METHOD` | Required for the keyboard service (IME) |

## License

Open source. Use it however you like.
