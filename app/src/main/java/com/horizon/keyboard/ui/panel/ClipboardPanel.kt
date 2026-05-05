package com.horizon.keyboard.ui.panel

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.horizon.keyboard.R
import com.horizon.keyboard.data.ClipboardRepository
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Smart clipboard panel — history tracking, saved clips, search, live listener.
 *
 * Features:
 * - Live clipboard monitoring via [ClipboardManager.OnPrimaryClipChangedListener]
 * - Two tabs: History (auto-tracked) and Saved (user-bookmarked)
 * - Search/filter across clips
 * - Tap to paste, tap ★ to save, tap ✕ to delete
 * - Persistent storage via [ClipboardRepository]
 *
 * @param context Android context.
 * @param repository Persistent clipboard data repository.
 * @param onPaste Callback to insert text into the input field.
 * @param onShowKeyboard Callback when clipboard panel is closed.
 */
class ClipboardPanel(
    private val context: Context,
    private val repository: ClipboardRepository,
    private val onPaste: ((String) -> Unit)? = null,
    private val onShowKeyboard: () -> Unit = {}
) {

    var panel: LinearLayout? = null
        private set

    private var historyContainer: LinearLayout? = null
    private var savedContainer: LinearLayout? = null
    private var searchInput: EditText? = null
    private var historyTab: TextView? = null
    private var savedTab: TextView? = null
    private var historyCountBadge: TextView? = null
    private var savedCountBadge: TextView? = null
    private var emptyStateText: TextView? = null

    private var showingSaved = false
    private var searchQuery = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    // Clipboard listener
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var clipboardManager: ClipboardManager? = null

    private fun dp(value: Int): Int = Dimensions.dp(context, value)

    // ─── Panel Creation ──────────────────────────────────────────

    fun createPanel(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(260))
            setBackgroundColor(Color.parseColor(Colors.BG_DARK))
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
            visibility = View.GONE
        }

        // ── Header Row ──────────────────────────────────────
        panel.addView(createHeader())

        // ── Search Bar ──────────────────────────────────────
        panel.addView(createSearchBar())

        // ── Tab Row (History | Saved) ───────────────────────
        panel.addView(createTabRow())

        // ── Clip List (scrollable) ──────────────────────────
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = true
        }

        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Empty state
        emptyStateText = TextView(context).apply {
            text = "📋 Copy text anywhere to track it here"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            val pad = dp(24)
            setPadding(0, pad, 0, pad)
            visibility = View.GONE
        }
        scrollContent.addView(emptyStateText)

        // History list
        historyContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollContent.addView(historyContainer)

        // Saved list (hidden by default)
        savedContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
        scrollContent.addView(savedContainer)

        scrollView.addView(scrollContent)
        panel.addView(scrollView)

        this.panel = panel

        // Start clipboard listener
        startClipboardListener()

        return panel
    }

    // ─── Header ──────────────────────────────────────────────────

    private fun createHeader(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(32))
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            setImageResource(R.drawable.ic_clipboard)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        header.addView(TextView(context).apply {
            text = "  CLIPBOARD"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        })

        // Clear button
        header.addView(textButton("Clear") {
            if (showingSaved) repository.clearSaved() else repository.clearHistory()
            refreshList()
        })

        // Close button
        header.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(8) }
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor(Colors.TEXT_TERTIARY))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener {
                panel?.visibility = View.GONE
                onShowKeyboard()
            }
        })

        return header
    }

    // ─── Search Bar ──────────────────────────────────────────────

    private fun createSearchBar(): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(32)
            ).apply { bottomMargin = dp(6) }
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(Colors.BG_KEY))
                cornerRadius = dp(8).toFloat()
            }
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
        }

        container.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
            setImageResource(R.drawable.ic_search)
            setColorFilter(Color.parseColor(Colors.TEXT_TERTIARY))
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        searchInput = EditText(context).apply {
            hint = "Search clips..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setBackgroundResource(0)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            val pad = dp(6)
            setPadding(pad, 0, 0, 0)
        }
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                refreshList()
            }
        })
        container.addView(searchInput)

        return container
    }

    // ─── Tab Row ─────────────────────────────────────────────────

    private fun createTabRow(): LinearLayout {
        val tabRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)
            ).apply { bottomMargin = dp(6) }
        }

        historyTab = tabButton("📋 History", true) {
            showingSaved = false
            updateTabStyles()
            refreshList()
        }
        historyCountBadge = countBadge()
        val historyTabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        historyTabContainer.addView(historyTab)
        historyTabContainer.addView(historyCountBadge)
        tabRow.addView(historyTabContainer)

        savedTab = tabButton("⭐ Saved", false) {
            showingSaved = true
            updateTabStyles()
            refreshList()
        }
        savedCountBadge = countBadge()
        val savedTabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        savedTabContainer.addView(savedTab)
        savedTabContainer.addView(savedCountBadge)
        tabRow.addView(savedTabContainer)

        return tabRow
    }

    // ─── Panel Visibility ────────────────────────────────────────

    fun show() {
        panel?.visibility = View.VISIBLE
        refreshList()
    }

    fun hide() {
        panel?.visibility = View.GONE
    }

    val isVisible: Boolean
        get() = panel?.visibility == View.VISIBLE

    // ─── Clipboard Listener ──────────────────────────────────────

    private fun startClipboardListener() {
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    mainHandler.post {
                        if (repository.addToHistory(text)) {
                            refreshList()
                        }
                    }
                }
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    fun stopClipboardListener() {
        clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        clipboardListener = null
    }

    // ─── Clipboard Tracking (legacy compat) ──────────────────────

    fun onClipboardChanged(text: String) {
        if (repository.addToHistory(text)) {
            if (isVisible) refreshList()
        }
    }

    // ─── Refresh ─────────────────────────────────────────────────

    private fun refreshList() {
        val historyClips = repository.searchHistory(searchQuery)
        val savedClips = repository.searchSaved(searchQuery)

        historyCountBadge?.text = "${repository.history.size}"
        savedCountBadge?.text = "${repository.saved.size}"

        if (showingSaved) {
            historyContainer?.visibility = View.GONE
            savedContainer?.visibility = View.VISIBLE
            renderClipList(savedContainer, savedClips, isSaved = true)
            emptyStateText?.visibility = if (savedClips.isEmpty()) View.VISIBLE else View.GONE
            emptyStateText?.text = if (searchQuery.isNotEmpty()) "No saved clips match \"$searchQuery\""
                else "⭐ Tap ★ on any clip to save it here"
        } else {
            historyContainer?.visibility = View.VISIBLE
            savedContainer?.visibility = View.GONE
            renderClipList(historyContainer, historyClips, isSaved = false)
            emptyStateText?.visibility = if (historyClips.isEmpty()) View.VISIBLE else View.GONE
            emptyStateText?.text = if (searchQuery.isNotEmpty()) "No history clips match \"$searchQuery\""
                else "📋 Copy text anywhere to track it here"
        }
    }

    private fun renderClipList(container: LinearLayout?, clips: List<String>, isSaved: Boolean) {
        container ?: return
        container.removeAllViews()

        clips.forEachIndexed { index, clipText ->
            container.addView(createClipItem(clipText, index, isSaved))
        }
    }

    // ─── Clip Item ───────────────────────────────────────────────

    private fun createClipItem(text: String, index: Int, isSaved: Boolean): LinearLayout {
        val item = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
            val pad = dp(10)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isSaved) Colors.SAVED_CLIP_BG else Colors.BG_KEY))
                cornerRadius = dp(6).toFloat()
                if (isSaved) setStroke(dp(1), Color.parseColor(Colors.SAVED_CLIP_BORDER))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        // Clip text (truncated)
        item.addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        // Save/Unsave button (★)
        val isAlreadySaved = repository.isSaved(text)
        item.addView(actionButton(
            if (isSaved) "★" else if (isAlreadySaved) "★" else "☆",
            if (isSaved || isAlreadySaved) Color.parseColor(Colors.ACCENT_ORANGE) else Color.parseColor(Colors.TEXT_TERTIARY)
        ) {
            if (isSaved) {
                repository.removeFromSaved(index)
            } else if (!isAlreadySaved) {
                repository.saveClip(text)
            }
            refreshList()
        })

        // Paste button
        item.addView(actionButton("📋", Color.parseColor(Colors.ACCENT_BLUE)) {
            onPaste?.invoke(text)
            hide()
            onShowKeyboard()
        })

        // Delete button
        item.addView(actionButton("✕", Color.parseColor(Colors.TEXT_TERTIARY)) {
            if (isSaved) repository.removeFromSaved(index) else repository.removeFromHistory(index)
            refreshList()
        })

        return item
    }

    // ─── UI Helpers ──────────────────────────────────────────────

    private fun tabButton(label: String, selected: Boolean, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = label
            setTextColor(Color.parseColor(if (selected) Colors.ACCENT_BLUE else Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun countBadge(): TextView {
        return TextView(context).apply {
            text = "0"
            setTextColor(Color.parseColor(Colors.TEXT_TERTIARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setPadding(dp(4), 0, dp(8), 0)
        }
    }

    private fun updateTabStyles() {
        historyTab?.setTextColor(Color.parseColor(if (!showingSaved) Colors.ACCENT_BLUE else Colors.TEXT_TERTIARY))
        historyTab?.typeface = if (!showingSaved) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        savedTab?.setTextColor(Color.parseColor(if (showingSaved) Colors.ACCENT_ORANGE else Colors.TEXT_TERTIARY))
        savedTab?.typeface = if (showingSaved) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun textButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = label
            setTextColor(Color.parseColor(Colors.ACCENT_RED))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { onClick() }
        }
    }

    private fun actionButton(symbol: String, color: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { onClick() }
            addView(TextView(context).apply {
                this.text = symbol
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
            })
        }
    }

    // ─── Cleanup ─────────────────────────────────────────────────

    fun cleanup() {
        stopClipboardListener()
    }
}
