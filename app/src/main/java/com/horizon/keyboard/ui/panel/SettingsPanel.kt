package com.horizon.keyboard.ui.panel

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.horizon.keyboard.R
import com.horizon.keyboard.voice.VoiceLanguage
import com.horizon.keyboard.VoiceTranscriptionEngine
import com.horizon.keyboard.core.VoiceEngineType
import com.horizon.keyboard.data.KeyboardPreferences
import com.horizon.keyboard.data.SecureKeyStore
import com.horizon.keyboard.ui.setup.ApiKeyActivity
import com.horizon.keyboard.ui.theme.Dimensions
import com.horizon.keyboard.ui.theme.Colors


/**
 * Settings panel UI — voice engine selection, language, API keys, model selection.
 *
 * Pure UI component. Data persistence handled by [KeyboardPreferences] and [SecureKeyStore].
 *
 * @param context Android context.
 * @param voiceEngine Voice engine to configure.
 * @param onShowKeyboard Callback when settings panel is closed.
 */
class SettingsPanel(
    private val context: Context,
    private val voiceEngine: VoiceTranscriptionEngine,
    private val onShowKeyboard: () -> Unit = {}
) {

    var voiceEngineType = VoiceEngineType.ANDROID_BUILTIN
    var selectedLanguage = VoiceLanguage.ENGLISH

    private val prefs = KeyboardPreferences(context)

    var panel: ScrollView? = null
        private set

    private fun dp(value: Int): Int = Dimensions.dp(context, value)

    // ─── Panel Creation ──────────────────────────────────────────

    fun createPanel(): ScrollView {
        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(320))
            setBackgroundColor(Color.parseColor(Colors.BG_DARK))
            visibility = View.GONE
        }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val pad = dp(10)
            setPadding(pad, pad, pad, pad)
        }

        scrollView.addView(panel)
        this.panel = scrollView
        return scrollView
    }

    // ─── Panel Visibility ────────────────────────────────────────

    fun show() {
        loadSettings()
        refreshPanel()
        panel?.visibility = View.VISIBLE
    }

    fun hide() {
        panel?.visibility = View.GONE
    }

    val isVisible: Boolean
        get() = panel?.visibility == View.VISIBLE

    // ─── Settings Persistence ────────────────────────────────────

    /** Last error message from load/save operations, or null if successful. */
    var lastError: String? = null
        private set

    fun loadSettings() {
        try {
            voiceEngineType = when (prefs.voiceEngine) {
                KeyboardPreferences.ENGINE_WHISPER_GROQ -> VoiceEngineType.WHISPER_GROQ
                KeyboardPreferences.ENGINE_GEMMA -> VoiceEngineType.GEMMA_API
                KeyboardPreferences.ENGINE_AUTO -> VoiceEngineType.AUTO
                else -> VoiceEngineType.ANDROID_BUILTIN
            }
            voiceEngine.groqApiKey = SecureKeyStore.getGroqKey(context)
            voiceEngine.gemmaApiKey = SecureKeyStore.getGemmaKey(context)
            voiceEngine.gemmaModelEn = prefs.gemmaModelEn
            voiceEngine.gemmaModelBn = prefs.gemmaModelBn
            selectedLanguage = VoiceLanguage.fromName(prefs.selectedLanguage)
            voiceEngine.currentVoiceLang = selectedLanguage.gemmaCode
            lastError = null
        } catch (e: Exception) {
            Log.e("SettingsPanel", "Failed to load settings", e)
            lastError = "Failed to load settings: ${e.message}"
        }
    }

    /** @return true if settings were saved successfully, false on error. */
    fun saveSettings(): Boolean {
        return try {
            prefs.voiceEngine = when (voiceEngineType) {
                VoiceEngineType.WHISPER_GROQ -> KeyboardPreferences.ENGINE_WHISPER_GROQ
                VoiceEngineType.GEMMA_API -> KeyboardPreferences.ENGINE_GEMMA
                VoiceEngineType.AUTO -> KeyboardPreferences.ENGINE_AUTO
                else -> KeyboardPreferences.ENGINE_ANDROID
            }
            prefs.gemmaModelEn = voiceEngine.gemmaModelEn
            prefs.gemmaModelBn = voiceEngine.gemmaModelBn
            prefs.selectedLanguage = selectedLanguage.name
            if (voiceEngine.groqApiKey.isNotEmpty()) SecureKeyStore.setGroqKey(context, voiceEngine.groqApiKey)
            if (voiceEngine.gemmaApiKey.isNotEmpty()) SecureKeyStore.setGemmaKey(context, voiceEngine.gemmaApiKey)
            lastError = null
            true
        } catch (e: Exception) {
            Log.e("SettingsPanel", "Failed to save settings", e)
            lastError = "Failed to save settings: ${e.message}"
            false
        }
    }

    // ─── Panel Refresh ───────────────────────────────────────────

    fun refreshPanel() {
        val scrollView = panel ?: return
        val panel = scrollView.getChildAt(0) as? LinearLayout ?: return
        panel.removeAllViews()

        // Title
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        titleRow.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            setImageResource(R.drawable.ic_settings)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        titleRow.addView(TextView(context).apply {
            text = "  SETTINGS"
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
        })
        panel.addView(titleRow)

        // Voice Engine Section
        panel.addView(sectionHeader("VOICE ENGINE"))

        val engines = listOf(
            "auto" to "Auto (Best for each language)",
            "whisper_groq" to "Whisper via Groq (English)",
            "gemma" to "Gemma 4 (Bangla)",
            "android" to "Android Built-in (Offline)"
        )
        engines.forEach { (key, label) ->
            val isSelected = (key == "auto" && voiceEngineType == VoiceEngineType.AUTO) ||
                (key == "whisper_groq" && voiceEngineType == VoiceEngineType.WHISPER_GROQ) ||
                (key == "gemma" && voiceEngineType == VoiceEngineType.GEMMA_API) ||
                (key == "android" && voiceEngineType == VoiceEngineType.ANDROID_BUILTIN)
            panel.addView(settingsOption(label, isSelected) {
                voiceEngineType = when (key) {
                    "auto" -> VoiceEngineType.AUTO
                    "whisper_groq" -> VoiceEngineType.WHISPER_GROQ
                    "gemma" -> VoiceEngineType.GEMMA_API
                    else -> VoiceEngineType.ANDROID_BUILTIN
                }
                saveSettings()
                refreshPanel()
            })
        }

        // Language Selection
        panel.addView(sectionHeader("VOICE LANGUAGE"))

        VoiceLanguage.entries.forEach { lang ->
            panel.addView(settingsOption(
                lang.displayName, voiceEngine.currentVoiceLang == lang.gemmaCode
            ) {
                voiceEngine.currentVoiceLang = lang.gemmaCode
                selectedLanguage = lang
                saveSettings()
                refreshPanel()
            })
        }

        // Groq API Key
        panel.addView(sectionHeader("GROQ API KEY (WHISPER)"))
        val groqStatus = if (voiceEngine.groqApiKey.isNotEmpty()) "✓ Key saved (tap to edit)" else "Tap to add key..."
        panel.addView(settingsOption(groqStatus, voiceEngine.groqApiKey.isNotEmpty()) {
            val intent = Intent(context, ApiKeyActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        })
        panel.addView(TextView(context).apply {
            text = "🔒 Encrypted with Android Keystore · Free: 2,000 RPD"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setPadding(dp(10), 0, 0, dp(4))
        })

        // Gemma API Key
        panel.addView(sectionHeader("GOOGLE AI STUDIO API KEY (GEMMA)"))
        val gemmaStatus = if (voiceEngine.gemmaApiKey.isNotEmpty()) "✓ Key saved (tap to edit)" else "Tap to add key..."
        panel.addView(settingsOption(gemmaStatus, voiceEngine.gemmaApiKey.isNotEmpty()) {
            val intent = Intent(context, ApiKeyActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        })
        panel.addView(TextView(context).apply {
            text = "🔒 Encrypted with Android Keystore · Free · Best for Bangla"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setPadding(dp(10), 0, 0, dp(4))
        })

        // Gemma Model (always visible)
        panel.addView(sectionHeader("GEMMA MODEL"))
        val models = listOf(
            "gemma-4-e4b-it" to "Gemma 4 E4B (4B — Better)",
            "gemma-4-e2b-it" to "Gemma 4 E2B (2B — Faster)"
        )
        models.forEach { (model, label) ->
            val isSelected = (voiceEngine.currentVoiceLang == "bn-BD" && voiceEngine.gemmaModelBn == model) ||
                (voiceEngine.currentVoiceLang == "en-US" && voiceEngine.gemmaModelEn == model)
            panel.addView(settingsOption(label, isSelected) {
                if (voiceEngine.currentVoiceLang == "bn-BD") voiceEngine.gemmaModelBn = model else voiceEngine.gemmaModelEn = model
                saveSettings()
                refreshPanel()
            })
        }

        // Close button
        val closeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
            setOnClickListener {
                this@SettingsPanel.panel?.visibility = View.GONE
                onShowKeyboard()
            }
        }
        closeRow.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor(Colors.TEXT_TERTIARY))
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        closeRow.addView(TextView(context).apply {
            text = " Close"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        panel.addView(closeRow)
    }

    // ─── UI Components ───────────────────────────────────────────

    private fun sectionHeader(title: String): TextView {
        return TextView(context).apply {
            text = title
            setTextColor(Color.parseColor(Colors.ACCENT_ORANGE))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(0, dp(10), 0, dp(4))
        }
    }

    private fun settingsOption(label: String, selected: Boolean, onClick: () -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)).apply { bottomMargin = dp(2) }
            gravity = Gravity.CENTER_VERTICAL
            val pad = dp(10)
            setPadding(pad, 0, pad, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (selected) Colors.SETTINGS_SELECTED_BG else Colors.BG_KEY))
                cornerRadius = dp(6).toFloat()
                if (selected) setStroke(dp(1), Color.parseColor(Colors.SETTINGS_SELECTED_BORDER))
            }
            setOnClickListener { onClick() }
        }

        container.addView(TextView(context).apply {
            text = if (selected) "● $label" else "   $label"
            setTextColor(Color.parseColor(if (selected) Colors.ACCENT_BLUE else Colors.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        })

        return container
    }

}
