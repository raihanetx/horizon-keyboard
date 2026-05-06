package com.horizon.keyboard.ui.bar

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Word suggestion bar displayed above the keyboard rows.
 *
 * Shows quick-tap word suggestions. When a word is tapped,
 * it's inserted followed by a space.
 *
 * Supports both static fallback suggestions and dynamic suggestions
 * from the SuggestionManager (dictionary + user learning + bigram).
 *
 * @param onWordSelected Callback when a suggestion word is tapped.
 *                       Receives the word with a trailing space.
 */
class SuggestionBar(
    private val context: Context,
    private val onWordSelected: (String) -> Unit
) {

    companion object {
        private val FALLBACK_SUGGESTIONS = listOf("I", "Hello", "The", "Thanks", "How")
    }

    /** The bar view for external reference. */
    lateinit var view: LinearLayout
        private set

    /** Current word views for updating. */
    private val wordViews = mutableListOf<TextView>()

    /**
     * Create and return the suggestion bar.
     */
    fun create(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Dimensions.dp(context, 40)
            ).apply {
                bottomMargin = Dimensions.dp(context, 4)
            }
            setBackgroundColor(Color.parseColor("#1E1E20"))
            val p = Dimensions.dp(context, 6)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Create 5 word slots
        for (i in 0 until 5) {
            val tv = createWordView("")
            wordViews.add(tv)
            bar.addView(tv)

            if (i < 4) {
                bar.addView(createDivider())
            }
        }

        // Load fallback suggestions
        updateSuggestions(FALLBACK_SUGGESTIONS)

        view = bar
        return bar
    }

    /**
     * Update the suggestion bar with new words.
     * Called by KeyboardView on every keystroke via SuggestionManager.
     *
     * @param words List of suggestion words (up to 5).
     */
    fun updateSuggestions(words: List<String>) {
        for (i in 0 until 5) {
            val tv = wordViews.getOrNull(i) ?: continue
            if (i < words.size) {
                tv.text = words[i]
                tv.visibility = View.VISIBLE
                tv.setOnClickListener { onWordSelected("${words[i]} ") }
            } else {
                tv.text = ""
                tv.visibility = View.INVISIBLE
            }
        }
    }

    private fun createWordView(word: String): TextView {
        return TextView(context).apply {
            text = word
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v as TextView).apply {
                            setTextColor(Color.WHITE)
                            background = GradientDrawable().apply {
                                setColor(Color.parseColor(Colors.BG_PILL))
                                cornerRadius = Dimensions.dp(context, 6).toFloat()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).apply {
                            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                            background = null
                        }
                        if (event.action == MotionEvent.ACTION_UP && text.isNotEmpty()) {
                            onWordSelected("$text ")
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun createDivider(): android.view.View {
        return android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                Dimensions.dp(context, 1),
                Dimensions.dp(context, 22)
            )
            setBackgroundColor(Color.parseColor(Colors.DIVIDER))
        }
    }
}
