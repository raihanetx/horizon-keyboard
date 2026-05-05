package com.horizon.keyboard.ui.bar

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.KeyboardTheme
import com.horizon.keyboard.R

/**
 * Voice recording bar shown in the header area when voice mode is active.
 *
 * Contains: language toggle (EN/BN) | status text | action buttons (stop, exit, start).
 * Replaces the header bar with a fade animation.
 *
 * @param context Android context.
 * @param onToggleLanguage Callback when language button is tapped.
 * @param onStartListening Callback when mic/start button is tapped.
 * @param onStopListening Callback when stop button is tapped.
 * @param onExit Callback when exit button is tapped.
 */
class VoiceBar(
    private val context: Context,
    private val onToggleLanguage: () -> Unit,
    private val onStartListening: () -> Unit,
    private val onStopListening: () -> Unit,
    private val onExit: () -> Unit
) {

    /** The voice bar view. Add to the header voice slot. */
    lateinit var view: LinearLayout
        private set

    /** Status text display (e.g. "Listening...", "Transcribing..."). */
    lateinit var statusText: TextView
        private set

    /** Language toggle button text (shows "EN" or "BN"). */
    lateinit var langButtonText: TextView
        private set

    // Internal button references
    private lateinit var stopBtn: ImageView
    private lateinit var exitBtn: ImageView
    private lateinit var startBtn: ImageView

    /**
     * Create and return the voice bar (hidden by default).
     * Call [show] to make it visible.
     */
    fun create(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(KeyboardTheme.BG_KEY))
            val pad = KeyboardTheme.dp(context, 8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            alpha = 0f
        }

        // ── Language Toggle ──────────────────────────────────
        val langContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 56),
                KeyboardTheme.dp(context, 28)
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor(KeyboardTheme.BG_DARK))
                cornerRadius = KeyboardTheme.dp(context, 14).toFloat()
                setStroke(KeyboardTheme.dp(context, 1), Color.parseColor(KeyboardTheme.BG_PILL))
            }
            setOnClickListener { onToggleLanguage() }
        }
        langContainer.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 14),
                KeyboardTheme.dp(context, 14)
            )
            setImageResource(R.drawable.ic_globe)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        langButtonText = TextView(context).apply {
            text = "EN"
            setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(KeyboardTheme.dp(context, 4), 0, 0, 0)
        }
        langContainer.addView(langButtonText)
        bar.addView(langContainer)

        // ── Status Text ──────────────────────────────────────
        statusText = TextView(context).apply {
            text = "Tap mic to start"
            setTextColor(Color.parseColor(KeyboardTheme.TEXT_DIM))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        bar.addView(statusText)

        // ── Button Container ─────────────────────────────────
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                KeyboardTheme.dp(context, 28)
            )
        }

        // Stop button (red X)
        stopBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 24),
                KeyboardTheme.dp(context, 24)
            )
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor(KeyboardTheme.ACCENT_RED))
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setOnClickListener { onStopListening() }
        }
        buttonContainer.addView(stopBtn)

        // Exit button (keyboard dismiss)
        exitBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 24),
                KeyboardTheme.dp(context, 24)
            ).apply { marginStart = KeyboardTheme.dp(context, 8) }
            setImageResource(R.drawable.ic_keyboard_dismiss)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setOnClickListener { onExit() }
        }
        buttonContainer.addView(exitBtn)

        // Start button (green mic)
        startBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 28),
                KeyboardTheme.dp(context, 28)
            )
            setImageResource(R.drawable.ic_voice)
            setColorFilter(Color.parseColor(KeyboardTheme.ACCENT_GREEN))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { onStartListening() }
        }
        buttonContainer.addView(startBtn)

        bar.addView(buttonContainer)
        view = bar
        return bar
    }

    // ─── Visibility ──────────────────────────────────────────────

    fun show() {
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).setDuration(FADE_DURATION).start()
    }

    fun hide() {
        view.animate().alpha(0f).setDuration(FADE_DURATION).withEndAction {
            view.visibility = View.GONE
        }.start()
    }

    // ─── State Updates ───────────────────────────────────────────

    /**
     * Update the visual state based on whether we're actively listening.
     * When listening: show stop + exit buttons, hide start button.
     * When idle: show start button, hide stop + exit.
     */
    fun updateListeningState(listening: Boolean) {
        if (listening) {
            startBtn.visibility = View.GONE
            stopBtn.visibility = View.VISIBLE
            exitBtn.visibility = View.VISIBLE
        } else {
            startBtn.visibility = View.VISIBLE
            stopBtn.visibility = View.GONE
            exitBtn.visibility = View.GONE
        }
    }

    /**
     * Update the language button text.
     * @param code "EN" or "BN".
     */
    fun updateLanguageLabel(code: String) {
        langButtonText.text = code
    }

    /**
     * Update the status text with a message and optional color.
     * @param message Status message to display.
     * @param colorHex Optional hex color (null = keep current).
     */
    fun updateStatus(message: String, colorHex: String? = null) {
        statusText.text = message
        if (colorHex != null) {
            statusText.setTextColor(Color.parseColor(colorHex))
        }
    }

    companion object {
        private const val FADE_DURATION = 200L
    }
}
