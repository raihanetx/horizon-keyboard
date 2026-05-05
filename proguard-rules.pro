# ============================================================
# Horizon Keyboard — ProGuard Rules
# ============================================================

# ─── App Entry Points ────────────────────────────────────────

# IME Service — referenced by AndroidManifest.xml
-keep class com.horizon.keyboard.HorizonKeyboardService { *; }

# Launcher Activity — referenced by AndroidManifest.xml
-keep class com.horizon.keyboard.ui.setup.MainActivity { *; }

# ─── Android Keystore / EncryptedSharedPreferences ───────────

# EncryptedSharedPreferences uses reflection internally
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ─── Speech Recognition ──────────────────────────────────────

# SpeechRecognizer callback interfaces
-keep class android.speech.RecognitionListener { *; }
-keep class android.speech.SpeechRecognizer { *; }

# ─── Kotlin ──────────────────────────────────────────────────

# Kotlin metadata for sealed classes and enums
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations

# Keep enum entries (VoiceEngineType, VoiceLanguage, KeyboardMode)
-keepclassmembers enum ** {
    **[] $VALUES;
    public *;
}

# Keep sealed class subtypes (KeyboardMode)
-keep class com.horizon.keyboard.core.KeyboardMode { *; }
-keep class com.horizon.keyboard.core.KeyboardMode$* { *; }

# ─── Compose (debug only, but safe to keep) ─────────────────

-dontwarn androidx.compose.**

# ─── General ─────────────────────────────────────────────────

# Remove logging in release (but keep Log.e for error paths)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Suppress warnings for missing classes
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn kotlin.Unit
-dontwarn kotlin.jvm.internal.**
