package com.horizon.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

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

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)
        buildKeyboard()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildKeyboard() {
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
        // Update all key labels
        updateKeyLabels()
    }

    private fun updateKeyLabels() {
        fun updateRow(view: View) {
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child is TextView) {
                        val label = child.text.toString()
                        if (label.length == 1 && label[0].isLetter()) {
                            child.text = if (isShift) label.uppercase() else label.lowercase()
                        }
                    } else if (child is ViewGroup) {
                        updateRow(child)
                    }
                }
            }
        }
        updateRow(this)

        // Update shift key visual
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
