package com.horizon.keyboard.ui.panel

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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.horizon.keyboard.R
import com.horizon.keyboard.data.ClipboardRepository
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Clipboard panel UI — history tracking, saved clips, paste/delete actions.
 *
 * Pure UI component. Data managed by [ClipboardRepository].
 *
 * @param context Android context.
 * @param repository Clipboard data repository.
 * @param onPaste Callback when paste button is tapped.
 * @param onShowKeyboard Callback when clipboard panel is closed.
 */
class ClipboardPanel(
    private val context: Context,
    private val repository: ClipboardRepository = ClipboardRepository(),
    private val onPaste: ((String) -> Unit)? = null,
    private val onShowKeyboard: () -> Unit = {}
) {

    var panel: LinearLayout? = null
        private set
    private var clipboardListContainer: LinearLayout? = null
    private var savedClipboardListContainer: LinearLayout? = null

    private fun dp(value: Int): Int = Dimensions.dp(context, value)

    // ─── Panel Creation ──────────────────────────────────────────

    fun createPanel(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(200))
            setBackgroundColor(Color.parseColor(Colors.BG_DARK))
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
            visibility = View.GONE
        }

        // Header row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(30))
            gravity = Gravity.CENTER_VERTICAL
        }

        headerRow.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            setImageResource(R.drawable.ic_clipboard)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        headerRow.addView(TextView(context).apply {
            text = "  CLIPBOARD"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        })

        headerRow.addView(TextView(context).apply {
            text = "Clear All"
            setTextColor(Color.parseColor(Colors.ACCENT_RED))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setOnClickListener {
                repository.clearHistory()
                refreshPanel()
            }
        })

        headerRow.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(8) }
            setImageResource(R.drawable.ic_close)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener {
                panel.visibility = View.GONE
                onShowKeyboard()
            }
        })

        panel.addView(headerRow)

        // Scroll content
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        clipboardListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        scrollContent.addView(clipboardListContainer)

        // Saved clips header
        val savedHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)).apply { topMargin = dp(8) }
            gravity = Gravity.CENTER_VERTICAL
        }
        savedHeader.addView(TextView(context).apply {
            text = "⭐ SAVED CLIPS"
            setTextColor(Color.parseColor(Colors.ACCENT_ORANGE))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
        })
        scrollContent.addView(savedHeader)

        savedClipboardListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        scrollContent.addView(savedClipboardListContainer)

        scrollView.addView(scrollContent)
        panel.addView(scrollView)

        this.panel = panel
        loadInitialClipboard()
        return panel
    }

    // ─── Panel Visibility ────────────────────────────────────────

    fun show() {
        loadInitialClipboard()
        panel?.visibility = View.VISIBLE
    }

    fun hide() {
        panel?.visibility = View.GONE
    }

    val isVisible: Boolean
        get() = panel?.visibility == View.VISIBLE

    // ─── Clipboard Tracking ──────────────────────────────────────

    fun onClipboardChanged(text: String) {
        if (repository.addToHistory(text)) {
            if (isVisible) refreshPanel()
        }
    }

    // ─── Private ─────────────────────────────────────────────────

    private fun loadInitialClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                repository.addToHistory(text)
            }
        }
        refreshPanel()
    }

    private fun refreshPanel() {
        refreshClipList(clipboardListContainer, repository.history, isSaved = false)
        refreshClipList(savedClipboardListContainer, repository.saved, isSaved = true)
    }

    private fun refreshClipList(container: LinearLayout?, clips: List<String>, isSaved: Boolean) {
        container ?: return
        container.removeAllViews()

        if (clips.isEmpty()) {
            container.addView(TextView(context).apply {
                text = if (isSaved) "Long press any clip to save it here"
                else "No clips yet.\nCopy text anywhere to track it here."
                setTextColor(Color.parseColor(if (isSaved) "#48484A" else Colors.TEXT_TERTIARY))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSaved) 10f else 11f)
                gravity = Gravity.CENTER
                val pad = dp(if (isSaved) 8 else 12)
                setPadding(0, pad, 0, pad)
            })
            return
        }

        clips.forEachIndexed { index, clipText ->
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(4)
                }
                val pad = dp(10)
                setPadding(pad, pad, pad, pad)
                background = if (isSaved) {
                    GradientDrawable().apply {
                        setColor(Color.parseColor(Colors.SAVED_CLIP_BG))
                        cornerRadius = dp(6).toFloat()
                        setStroke(dp(1), Color.parseColor(Colors.SAVED_CLIP_BORDER))
                    }
                } else {
                    GradientDrawable().apply {
                        setColor(Color.parseColor(Colors.BG_KEY))
                        cornerRadius = dp(6).toFloat()
                    }
                }
                gravity = Gravity.CENTER_VERTICAL
            }

            if (isSaved) {
                item.addView(TextView(context).apply {
                    text = "⭐"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setPadding(0, 0, dp(6), 0)
                })
            }

            item.addView(TextView(context).apply {
                text = clipText
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Long press to save (only for history clips)
            if (!isSaved) {
                item.isLongClickable = true
                item.setOnLongClickListener {
                    if (repository.saveClip(clipText)) {
                        refreshPanel()
                        Toast.makeText(context, "⭐ Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Already saved", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }

            // Paste button
            val pasteContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(8), 0, 0, 0)
                setOnClickListener {
                    onPaste?.invoke(clipText)
                    hide()
                    onShowKeyboard()
                }
            }
            pasteContainer.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
                setImageResource(R.drawable.ic_paste)
                if (isSaved) setColorFilter(Color.parseColor(Colors.ACCENT_ORANGE))
                scaleType = ImageView.ScaleType.FIT_CENTER
            })
            item.addView(pasteContainer)

            // Delete button
            val deleteContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(8), 0, 0, 0)
                setOnClickListener {
                    if (isSaved) repository.removeFromSaved(index) else repository.removeFromHistory(index)
                    refreshPanel()
                }
            }
            deleteContainer.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
                setImageResource(R.drawable.ic_close)
                setColorFilter(Color.parseColor(Colors.TEXT_TERTIARY))
                scaleType = ImageView.ScaleType.FIT_CENTER
            })
            item.addView(deleteContainer)

            container.addView(item)
        }
    }
}
