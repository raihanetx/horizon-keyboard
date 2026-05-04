package com.horizon.keyboard

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Wraps Jetpack Compose keyboard UI for use in InputMethodService.
 *
 * InputMethodService doesn't set up LifecycleOwner on its window, causing
 * "ViewTreeLifecycleOwner not found" when Compose tries to create a WindowRecomposer.
 *
 * Fix: Implements LifecycleOwner + SavedStateRegistryOwner and installs them
 * on the window's decor view so Compose can find them at any level of the hierarchy.
 */
class ComposeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr), LifecycleOwner, SavedStateRegistryOwner {

    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onPaste: ((String) -> Unit)? = null

    // --- Lifecycle plumbing ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        setBackgroundColor(Color.parseColor("#1C1C1E"))

        // Install lifecycle on this view
        setViewTreeLifecycleOwner(this)
        setViewTreeSavedStateRegistryOwner(this)

        // Initialize lifecycle to CREATED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @Composable
    override fun Content() {
        HorizonKeyboardUI(
            onKeyPress = { onKeyPress?.invoke(it) },
            onBackspace = { onBackspace?.invoke() },
            onEnter = { onEnter?.invoke() },
            onShift = { },
            onSpace = { onSpace?.invoke() },
            onSymbol = { }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // CRITICAL: Install lifecycle owner on the window's decor view.
        // Compose's WindowRecomposer walks up from any view to find the lifecycle owner.
        // In InputMethodService, the window doesn't have one, so we install it.
        installLifecycleOnWindowDecor()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDetachedFromWindow()
    }

    private fun installLifecycleOnWindowDecor() {
        // Post to ensure the view is fully attached to the window
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                try {
                    // Walk up to the root of the view hierarchy (the window's decor view)
                    var current: View = this@ComposeKeyboardView
                    while (current.parent is ViewGroup) {
                        current = current.parent as ViewGroup
                    }
                    // current is now the decor view (or the highest ViewGroup we can reach)
                    // Install lifecycle owner on it so ALL children (including ComposeView)
                    // can find it when walking up the tree
                    current.setViewTreeLifecycleOwner(this@ComposeKeyboardView)
                    current.setViewTreeSavedStateRegistryOwner(this@ComposeKeyboardView)
                } catch (_: Exception) {
                    // Best effort — at least we have it on our own view
                }
            }
        })
    }

}
