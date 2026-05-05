package com.horizon.keyboard.ui.bar

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.KeyboardTheme

/**
 * Word suggestion bar displayed above the keyboard rows.
 *
 * Shows quick-tap word suggestions. When a word is tapped,
 * it's inserted followed by a space.
 *
 * @param onWordSelected Callback when a suggestion word is tapped.
 *                       Receives the word with a trailing space.
 */
class SuggestionBar(
    private val context: Context,
    private val onWordSelected: (String) -> Unit
) {

    companion object {
        private val SUGGESTIONS = listOf("I", "Hello", "The", "Thanks", "How")
    }

    /**
     * Create and return the suggestion bar.
     */
    fun create(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                KeyboardTheme.dp(context, 40)
            ).apply {
                bottomMargin = KeyboardTheme.dp(context, 4)
            }
            setBackgroundColor(Color.parseColor("#1E1E20"))
            val p = KeyboardTheme.dp(context, 6)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        SUGGESTIONS.forEachIndexed { index, word ->
            val tv = createWordView(word)

            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v as TextView).apply {
                            setTextColor(Color.WHITE)
                            background = GradientDrawable().apply {
                                setColor(Color.parseColor(KeyboardTheme.BG_PILL))
                                cornerRadius = KeyboardTheme.dp(context, 6).toFloat()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).apply {
                            setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
                            background = null
                        }
                        if (event.action == MotionEvent.ACTION_UP) {
                            onWordSelected("$word ")
                        }
                        true
                    }
                    else -> false
                }
            }

            bar.addView(tv)

            // Divider between words
            if (index < SUGGESTIONS.size - 1) {
                bar.addView(createDivider())
            }
        }

        return bar
    }

    private fun createWordView(word: String): TextView {
        return TextView(context).apply {
            text = word
            setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
    }

    private fun createDivider(): android.view.View {
        return android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                KeyboardTheme.dp(context, 1),
                KeyboardTheme.dp(context, 22)
            )
            setBackgroundColor(Color.parseColor(KeyboardTheme.DIVIDER))
        }
    }
}
