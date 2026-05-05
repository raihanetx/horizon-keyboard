package com.horizon.keyboard.ui.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.R
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Drawables
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Factory for creating keyboard key views.
 *
 * Handles all key creation: letter keys, special keys, icon keys,
 * backspace, enter, and symbol keys. Each key has its own touch
 * handler with haptic feedback.
 *
 * @param context Android context.
 * @param onKeyPress Callback when a character key is pressed.
 * @param onBackspace Callback when backspace is pressed.
 * @param onEnter Callback when enter is pressed.
 * @param onSpace Callback when space is pressed.
 * @param allLetterKeys Mutable list to track letter keys for shift toggling.
 * @param getIsShift Lambda to read current shift state.
 * @param onToggleShift Callback to toggle shift after a letter press.
 */
class KeyViewFactory(
    private val context: Context,
    private val onKeyPress: ((String) -> Unit)?,
    private val onBackspace: (() -> Unit)?,
    private val onEnter: (() -> Unit)?,
    private val onSpace: (() -> Unit)?,
    private val allLetterKeys: MutableList<TextView>,
    private val getIsShift: () -> Boolean,
    private val onToggleShift: () -> Unit
) {

    /** Set by coordinator after construction — needed for popup positioning. */
    var mainContentContainer: FrameLayout? = null

    // ─── Long Press Backspace ────────────────────────────────────

    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val BACKSPACE_INITIAL_DELAY = 400L
    private val BACKSPACE_REPEAT_DELAY = 70L

    // ─── Enter Key Reference ─────────────────────────────────────

    private var enterKeyView: TextView? = null

    // ─── Base Key View ───────────────────────────────────────────

    fun createKeyView(label: String, textSize: Float = 18f, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            gravity = Gravity.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            background = Drawables.keyBgNormal()
        }
    }

    // ─── Letter/Number Key ───────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun addKey(row: LinearLayout, label: String, textSize: Float = 18f) {
        val tv = createKeyView(label, textSize)
        tv.layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), 1f).apply {
            marginStart = Dimensions.dp(context, 3)
            marginEnd = Dimensions.dp(context, 3)
        }

        if (label[0].isLetter()) allLetterKeys.add(tv)

        val popup = KeyPopupManager.createPopup(context)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply { setTextColor(Color.WHITE); background = Drawables.keyBgPressed() }
                    KeyPopupManager.show(mainContentContainer, tv, popup)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply { setTextColor(Color.WHITE); background = Drawables.keyBgNormal() }
                    KeyPopupManager.hide(popup)
                    if (event.action == MotionEvent.ACTION_UP) {
                        val output = if (getIsShift()) label.uppercase() else label.lowercase()
                        onKeyPress?.invoke(output)
                        if (getIsShift() && label[0].isLetter()) onToggleShift()
                    }
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
    }

    // ─── Key With Icon (e.g. comma) ──────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun addKeyWithIcon(row: LinearLayout, label: String, iconRes: Int) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), 1f).apply {
                marginStart = Dimensions.dp(context, 3)
                marginEnd = Dimensions.dp(context, 3)
            }
            background = Drawables.keyBgNormal()
        }

        container.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Dimensions.dp(context, 16), Dimensions.dp(context, 16))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = Drawables.keyBgPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = Drawables.keyBgNormal()
                    if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke(label)
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    // ─── Backspace Key (with long press repeat) ──────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun addBackspaceKey(row: LinearLayout) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), 1.5f).apply {
                marginStart = Dimensions.dp(context, 3)
                marginEnd = Dimensions.dp(context, 3)
            }
            background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID)
        }

        container.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Dimensions.dp(context, 20), Dimensions.dp(context, 20))
            setImageResource(R.drawable.ic_backspace)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID_PRESSED)
                    onBackspace?.invoke()
                    backspaceRunnable = object : Runnable {
                        override fun run() {
                            onBackspace?.invoke()
                            backspaceHandler.postDelayed(this, BACKSPACE_REPEAT_DELAY)
                        }
                    }
                    backspaceHandler.postDelayed(backspaceRunnable!!, BACKSPACE_INITIAL_DELAY)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID)
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    // ─── Special Key (text label, e.g. "123", "SPACE", "ABC") ───

    @SuppressLint("ClickableViewAccessibility")
    fun addSpecialKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        bg: String? = null,
        textSize: Float = 14f,
        bold: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val tv = createKeyView(label, textSize, bold)
        tv.layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), weight).apply {
            marginStart = Dimensions.dp(context, 3)
            marginEnd = Dimensions.dp(context, 3)
        }

        val bgColor = bg ?: Colors.BG_KEY_SOLID
        tv.background = Drawables.keyBgSolid(bgColor)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).background = Drawables.keyBgSolid(bgColor)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
        return tv
    }

    // ─── Special Key With Icon (e.g. shift) ──────────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun addSpecialKeyWithIcon(
        row: LinearLayout,
        iconRes: Int,
        weight: Float,
        onClick: () -> Unit
    ): TextView {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), weight).apply {
                marginStart = Dimensions.dp(context, 3)
                marginEnd = Dimensions.dp(context, 3)
            }
            background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID)
        }

        container.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(Dimensions.dp(context, 20), Dimensions.dp(context, 20))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = Drawables.keyBgSolid(Colors.BG_KEY_SOLID_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = if (getIsShift()) Drawables.keyBgSolid(Colors.ACCENT_BLUE) else Drawables.keyBgSolid(Colors.BG_KEY_SOLID)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(container)

        // Return dummy TextView for shift state tracking
        return TextView(context).apply { tag = container }
    }

    // ─── Symbol Key ──────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun addSymbolKey(row: LinearLayout, label: String) {
        val tv = createKeyView(label)
        tv.layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), 1f).apply {
            marginStart = Dimensions.dp(context, 2)
            marginEnd = Dimensions.dp(context, 2)
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply { setTextColor(Color.WHITE); background = Drawables.keyBgPressed() }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply { setTextColor(Color.WHITE); background = Drawables.keyBgNormal() }
                    if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke(label)
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
    }

    // ─── Enter Key ───────────────────────────────────────────────

    fun createEnterKey(): TextView {
        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            compoundDrawablePadding = 0
            layoutParams = LinearLayout.LayoutParams(0, Dimensions.dp(context, 44), 2f).apply {
                marginStart = Dimensions.dp(context, 3)
                marginEnd = Dimensions.dp(context, 3)
            }
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    updateEnterKeyAppearance()
                    if (event.action == MotionEvent.ACTION_UP) onEnter?.invoke()
                    true
                }
                else -> false
            }
        }

        enterKeyView = tv
        return tv
    }

    fun updateEnterKeyAppearance(imeAction: Int = currentImeAction) {
        currentImeAction = imeAction
        val key = enterKeyView ?: return
        when (imeAction) {
            android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            }
            android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_send, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_GREEN)
            }
            android.view.inputmethod.EditorInfo.IME_ACTION_GO -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            }
            android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            }
            android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            }
            else -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = Drawables.keyBgSolid(Colors.ACCENT_BLUE)
            }
        }
    }

    private var currentImeAction: Int = android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED

    // ─── Cleanup ─────────────────────────────────────────────────

    fun cleanup() {
        backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
        backspaceRunnable = null
    }
}
