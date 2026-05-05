# Horizon Keyboard ProGuard Rules

# ─── Android Keystore + EncryptedSharedPreferences ───────────
# These use reflection internally — must keep their classes.
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ─── Jetpack Compose ─────────────────────────────────────────
# Compose uses runtime reflection for some features.
-dontwarn androidx.compose.**

# ─── VoiceTranscriptionEngine API calls ──────────────────────
# Uses java.net.HttpURLConnection — no special rules needed,
# but keep the API classes in case of future Gson/serialization.
-keep class com.horizon.keyboard.voice.api.** { *; }

# ─── General Android ─────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
