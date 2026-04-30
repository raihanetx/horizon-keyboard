# Horizon Keyboard

A minimal Android keyboard built with Jetpack Compose, featuring **voice typing** with support for **English** and **Bangla**.

## Features

### Keyboard
- **QWERTY layout** with smooth key press interactions
- **Shift** toggle for uppercase letters
- **Symbol/Number mode** (123 → symbol grid)
- **Space, Backspace, Enter** with editor action support
- Dark theme (iOS-style dark keyboard aesthetic)

### Voice Typing 🎤
- **English** (`en-US`) and **Bangla** (`bn-BD`) speech recognition
- **Monochrome box-beat visualizer** — 20 animated white bars that pulse when listening
- **Language toggle** — switch between English/Bangla on the fly
- **Start/Stop** control with visual state feedback
- **Partial results** displayed live as you speak
- Recognized text is committed directly to the input field
- Uses Android's `SpeechRecognizer` API (requires Google Speech Services)

### Setup Screen
- Step-by-step guide to enable and switch to Horizon Keyboard
- Microphone permission request on first launch

## Architecture

```
com.horizon.keyboard/
├── MainActivity.kt          # Setup screen + permission handling
├── HorizonKeyboardService.kt # InputMethodService (IME lifecycle)
├── KeyboardScreen.kt        # Main keyboard UI (Compose)
└── VoiceTypingBar.kt        # Voice typing UI + SpeechRecognizer
```

### Key Components

| File | Role |
|------|------|
| `HorizonKeyboardService` | Extends `InputMethodService` with Compose lifecycle support. Handles text input, backspace, enter, shift, space, and voice text commit. |
| `KeyboardScreen` | Composable function rendering the QWERTY keyboard with shift, symbol mode, and a 🎤 toggle button. |
| `VoiceTypingBar` | Composable implementing the monochrome voice bar — language selector, box-beat visualizer, and start/stop control. Uses `SpeechRecognizer` for real-time recognition. |
| `MainActivity` | Launcher activity showing setup instructions. Requests `RECORD_AUDIO` permission on launch. |

## Tech Stack

- **Kotlin** + **Jetpack Compose** (UI)
- **Android SDK 34** (compileSdk / targetSdk)
- **Min SDK 26** (Android 8.0+)
- **AGP 8.2.2** + **Gradle 8.5**
- **Kotlin 1.9.22** + **Compose BOM 2024.01.00**
- **Material 3** components

## Build

```bash
# Clone
git clone https://github.com/raihanetx/horizon-keyboard.git
cd horizon-keyboard

# Set up Android SDK (needs SDK 34 + build-tools 34.0.0)
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

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
2. Tap the **🎤** button on the keyboard
3. The voice bar appears with a **Start** button
4. Tap **Start** → speak in English or Bangla
5. Tap the language button to switch between **English** / **Bangla**
6. Recognized text is automatically inserted
7. Tap **▲ Hide Voice** to close the voice bar

## Permissions

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Required for voice typing / speech recognition |
| `BIND_INPUT_METHOD` | Required for the keyboard service (IME) |

## Voice Bar UI

The voice typing bar is a pixel-perfect Jetpack Compose translation of a monochrome design:

- **Background:** `#121212` (dark gray)
- **Elements:** Pure white `#FFFFFF`
- **Beat bars:** 20 box-type bars (6×22px) with snap-beat animation
- **Buttons:** Micro rounded-full with 0.5px hairline borders
- **Animation:** `cubic-bezier(.2, .9, .3, 1)` easing, randomized durations (350–800ms)

## License

Open source. Use it however you like.
