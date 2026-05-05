package com.horizon.keyboard.ui.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Manages key popup previews (the floating letter that appears when you tap a key).
 *
 * All methods are static — popups are stateless TextViews created on-demand.
 */
object KeyPopupManager {

    /**
     * Create a reusable popup TextView (hidden by default).
     */
    fun createPopup(context: android.content.Context): TextView {
        return TextView(context).apply {
            text = ""
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor(Colors.BG_PILL))
                cornerRadius = Dimensions.dp(context, 8).toFloat()
                setStroke(Dimensions.dp(context, 1), Color.parseColor("#5A5A5C"))
            }
            elevation = 12f
            visibility = View.GONE
        }
    }

    /**
     * Show a popup anchored above the given key view.
     *
     * @param container The [FrameLayout] to add the popup to (mainContentContainer).
     * @param anchor The key view to position the popup above.
     * @param popup The popup TextView (from [createPopup]).
     */
    fun show(container: FrameLayout?, anchor: View, popup: TextView) {
        try {
            val parent = anchor.parent as? ViewGroup ?: return
            if (popup.parent != null) (popup.parent as? ViewGroup)?.removeView(popup)

            popup.layoutParams = FrameLayout.LayoutParams(
                Dimensions.dp(anchor.context, 48),
                Dimensions.dp(anchor.context, 52)
            )
            popup.visibility = View.VISIBLE

            val parentFrame = parent as? FrameLayout
            if (parentFrame != null) {
                parentFrame.addView(popup)
                popup.x = anchor.left.toFloat()
                popup.y = anchor.top.toFloat() - Dimensions.dp(anchor.context, 54)
            } else {
                container?.addView(popup)
                val loc = IntArray(2)
                anchor.getLocationOnScreen(loc)
                val parentLoc = IntArray(2)
                container?.getLocationOnScreen(parentLoc)
                popup.x = (loc[0] - parentLoc[0]).toFloat()
                popup.y = (loc[1] - parentLoc[1]).toFloat() - Dimensions.dp(anchor.context, 54)
            }
        } catch (_: Exception) {}
    }

    /**
     * Hide and detach a popup.
     */
    fun hide(popup: TextView) {
        try {
            popup.visibility = View.GONE
            (popup.parent as? ViewGroup)?.removeView(popup)
        } catch (_: Exception) {}
    }
}
