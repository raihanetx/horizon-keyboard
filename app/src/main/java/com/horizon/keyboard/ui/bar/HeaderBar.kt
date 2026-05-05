package com.horizon.keyboard.ui.bar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.horizon.keyboard.R
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Drawables
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Top toolbar bar with keyboard logo and action icons.
 *
 * Contains: keyboard logo (left) + spacer + translate, clipboard, voice, settings icons (right).
 * Each icon button has haptic feedback and press/release visual states.
 *
 * @param onTranslate Callback when translate icon is tapped.
 * @param onClipboard Callback when clipboard icon is tapped.
 * @param onVoice Callback when voice icon is tapped.
 * @param onSettings Callback when settings icon is tapped.
 */
class HeaderBar(
    private val context: Context,
    private val onTranslate: () -> Unit,
    private val onClipboard: () -> Unit,
    private val onVoice: () -> Unit,
    private val onSettings: () -> Unit
) {

    /** The header bar view. Add this to your layout. */
    lateinit var view: LinearLayout
        private set

    /**
     * Create and return the header bar.
     * Must be called once before accessing [view].
     */
    fun create(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(Colors.BG_KEY))
            val pad = Dimensions.dp(context, 8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Keyboard logo
        header.addView(createIconImageView(R.drawable.ic_keyboard, Dimensions.dp(context, 20), tint = Colors.ACCENT_BLUE))

        // Spacer
        header.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        // Action icons
        val icons = listOf(
            R.drawable.ic_translate to onTranslate,
            R.drawable.ic_clipboard to onClipboard,
            R.drawable.ic_voice to onVoice,
            R.drawable.ic_settings to onSettings
        )

        icons.forEach { (drawableRes, action) ->
            header.addView(createHeaderIconButton(drawableRes, action))
        }

        view = header
        return header
    }

    // ─── Private: Icon Helpers ───────────────────────────────────

    private fun createIconImageView(drawableRes: Int, size: Int, tint: String? = null): ImageView {
        return ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(drawableRes)
            if (tint != null) setColorFilter(Color.parseColor(tint))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderIconButton(drawableRes: Int, onClick: () -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 36),
                Dimensions.dp(context, 32)
            ).apply {
                marginStart = Dimensions.dp(context, 3)
                marginEnd = Dimensions.dp(context, 3)
            }
            val p = Dimensions.dp(context, 6)
            setPadding(p, p, p, p)
            background = Drawables.pillBg(Colors.BG_PILL)
        }

        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 18),
                Dimensions.dp(context, 18)
            )
            setImageResource(drawableRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = Drawables.pillBg(Colors.BG_PILL_PRESSED)
                    iconView.setColorFilter(Color.WHITE)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = Drawables.pillBg(Colors.BG_PILL)
                    iconView.clearColorFilter()
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        return container
    }
}
