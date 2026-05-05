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
import com.horizon.keyboard.R
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions


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
            setBackgroundColor(Color.parseColor(Colors.BG_KEY))
            val pad = Dimensions.dp(context, 8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            alpha = 0f
        }

        // ── Language Toggle (left) ───────────────────────────
        val langContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 56),
                Dimensions.dp(context, 28)
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor(Colors.BG_DARK))
                cornerRadius = Dimensions.dp(context, 14).toFloat()
                setStroke(Dimensions.dp(context, 1), Color.parseColor(Colors.BG_PILL))
            }
            setOnClickListener { onToggleLanguage() }
        }
        langContainer.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 14),
                Dimensions.dp(context, 14)
            )
            setImageResource(R.drawable.ic_globe)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        langButtonText = TextView(context).apply {
            text = "EN"
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(Dimensions.dp(context, 4), 0, 0, 0)
        }
        langContainer.addView(langButtonText)
        bar.addView(langContainer)

        // ── "Listening" text (middle) ────────────────────────
        statusText = TextView(context).apply {
            text = "Listening"
            setTextColor(Color.parseColor(Colors.ACCENT_GREEN))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        bar.addView(statusText)

        // ── Stop button (right) — single button ──────────────
        stopBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 28),
                Dimensions.dp(context, 28)
            )
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor(Colors.ACCENT_RED))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { onStopListening() }
        }
        bar.addView(stopBtn)

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
     * Update the visual state — no-op since we always show the same layout.
     */
    fun updateListeningState(listening: Boolean) {
        // Always show the same UI — no toggling needed
    }

    /**
     * Update the language button text.
     * @param code "EN" or "BN".
     */
    fun updateLanguageLabel(code: String) {
        langButtonText.text = code
    }

    /**
     * Update the status text — always keeps "Listening" displayed.
     */
    fun updateStatus(message: String, colorHex: String? = null) {
        // Keep "Listening" static — don't change text
        statusText.text = "Listening"
        statusText.setTextColor(Color.parseColor(Colors.ACCENT_GREEN))
    }

    companion object {
        private const val FADE_DURATION = 200L
    }
}
