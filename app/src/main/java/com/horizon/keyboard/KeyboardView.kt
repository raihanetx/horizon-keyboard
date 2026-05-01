package com.horizon.keyboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.HorizontalScrollView

/**
 * Traditional View-based keyboard that works reliably in InputMethodService.
 * No Compose, no Recomposer, no lifecycle issues.
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null

    private var isShift = false
    private val shiftKeys = mutableListOf<TextView>()
    private val allLetterKeys = mutableListOf<TextView>()

    // Word suggestion bar
    private lateinit var suggestionBar: LinearLayout
    private lateinit var suggestionScroll: HorizontalScrollView
    private val suggestions = mutableListOf<String>()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)
        buildKeyboard()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildKeyboard() {
        // === Header Bar ===
        addView(createHeaderBar())

        // === Word Suggestion Bar ===
        addView(createSuggestionBar())

        // === Keyboard Layout ===
        // Row 1
        val row1 = createRow()
        "qwertyuiop".forEach { addKey(row1, it.toString()) }

        // Row 2
        val row2 = createRow()
        "asdfghjkl".forEach { addKey(row2, it.toString()) }

        // Row 3
        val row3 = createRow()
        addSpecialKey(row3, "⇧", 1.4f) { toggleShift() }.also { shiftKeys.add(it) }
        "zxcvbnm".forEach { addKey(row3, it.toString()) }
        addSpecialKey(row3, "⌫", 1.4f) { onBackspace?.invoke() }

        // Row 4
        val row4 = createRow()
        addSpecialKey(row4, "123", 1.4f, textSize = 11f) { /* no-op */ }
        addKey(row4, "@")
        addSpecialKey(row4, "SPACE", 5f, textSize = 11f) { onSpace?.invoke() }
        addKey(row4, ".")
        addSpecialKey(row4, "DONE", 2f, bg = "#0A84FF", textSize = 12f, bold = true) { onEnter?.invoke() }

        addView(row1)
        addView(row2)
        addView(row3)
        addView(row4)
    }

    // ─── Header Bar ──────────────────────────────────────────────

    private fun createHeaderBar(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(42)).apply {
                bottomMargin = dp(4)
            }
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            val p = dp(6)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Keyboard icon
        header.addView(createHeaderIcon("⌨", "#0A84FF"))

        // Spacer
        header.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        // Translate button
        header.addView(createHeaderButton("🌐", "Translate"))
        // Clipboard button
        header.addView(createHeaderButton("📋", "Clipboard"))
        // Voice button
        header.addView(createHeaderButton("🎤", "Voice"))
        // Settings button
        header.addView(createHeaderButton("⚙", "Settings"))

        return header
    }

    private fun createHeaderIcon(icon: String, color: String): TextView {
        return TextView(context).apply {
            text = icon
            setTextColor(Color.parseColor(color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderButton(icon: String, label: String): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(32)
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            val p = dp(8)
            setPadding(p, 0, p, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3A3A3C"))
                cornerRadius = dp(16).toFloat()
            }
        }

        val iconView = TextView(context).apply {
            text = icon
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#A0A0A8"))
            gravity = Gravity.CENTER
        }

        val labelView = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.parseColor("#A0A0A8"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.02f
        }

        container.addView(iconView)
        container.addView(labelView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as ViewGroup).background = GradientDrawable().apply {
                        setColor(Color.parseColor("#48484A"))
                        cornerRadius = dp(16).toFloat()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as ViewGroup).background = GradientDrawable().apply {
                        setColor(Color.parseColor("#3A3A3C"))
                        cornerRadius = dp(16).toFloat()
                    }
                    if (event.action == MotionEvent.ACTION_UP) {
                        showComingSoon(label)
                    }
                    true
                }
                else -> false
            }
        }

        return container
    }

    private fun showComingSoon(feature: String) {
        Toast.makeText(context, "$feature — Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    // ─── Suggestion Bar ──────────────────────────────────────────

    private fun createSuggestionBar(): View {
        suggestionBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        suggestionScroll = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(36)).apply {
                bottomMargin = dp(4)
            }
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            addView(suggestionBar)
        }

        // Default suggestions
        updateSuggestions(listOf("Hello", "The", "Thanks", "How", "What"))
        return suggestionScroll
    }

    @SuppressLint("ClickableViewAccessibility")
    fun updateSuggestions(words: List<String>) {
        suggestions.clear()
        suggestions.addAll(words)
        suggestionBar.removeAllViews()

        words.forEachIndexed { index, word ->
            val tv = TextView(context).apply {
                text = word
                setTextColor(Color.parseColor("#A0A0A8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    marginStart = dp(8)
                    marginEnd = dp(8)
                }
            }

            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v as TextView).setTextColor(Color.WHITE)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).setTextColor(Color.parseColor("#A0A0A8"))
                        if (event.action == MotionEvent.ACTION_UP) {
                            onKeyPress?.invoke("$word ")
                        }
                        true
                    }
                    else -> false
                }
            }

            suggestionBar.addView(tv)

            // Add divider between items
            if (index < words.size - 1) {
                suggestionBar.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(1), dp(18)).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    setBackgroundColor(Color.parseColor("#3A3A3C"))
                })
            }
        }
    }

    // ─── Keyboard Rows ───────────────────────────────────────────

    private fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(46)).apply {
                bottomMargin = dp(5)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addKey(row: LinearLayout, label: String) {
        val tv = createKeyView(label)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        if (label[0].isLetter()) {
            allLetterKeys.add(tv)
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).setTextColor(Color.WHITE)
                    v.background = keyBg("#48484A")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).setTextColor(Color.WHITE)
                    v.background = keyBgGradient()
                    if (event.action == MotionEvent.ACTION_UP) {
                        val output = if (isShift) label.uppercase() else label.lowercase()
                        onKeyPress?.invoke(output)
                        if (isShift) toggleShift()
                    }
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpecialKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        bg: String? = null,
        textSize: Float = 14f,
        bold: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val tv = createKeyView(label, textSize, bold)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(46), weight).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        val bgColor = bg ?: "#48484A"
        tv.background = keyBg(bgColor)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).background = keyBg("#636366")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).background = keyBg(bgColor)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
        return tv
    }

    private fun createKeyView(label: String, textSize: Float = 18f, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            gravity = Gravity.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            background = keyBgGradient()
        }
    }

    private fun toggleShift() {
        isShift = !isShift
        updateKeyLabels()
    }

    private fun updateKeyLabels() {
        allLetterKeys.forEach { child ->
            val label = child.text.toString()
            if (label.length == 1 && label[0].isLetter()) {
                child.text = if (isShift) label.uppercase() else label.lowercase()
            }
        }

        shiftKeys.forEach {
            it.background = if (isShift) keyBg("#0A84FF") else keyBg("#48484A")
        }
    }

    private fun keyBg(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun keyBgGradient(): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(
                Color.parseColor("#3A3A3C"),
                Color.parseColor("#2C2C2E")
            )
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
