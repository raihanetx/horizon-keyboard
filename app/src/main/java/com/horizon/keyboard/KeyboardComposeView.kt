package com.horizon.keyboard

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*

/**
 * A ComposeView for IME that manages its own Recomposer.
 *
 * Regular ComposeView crashes in InputMethodService because it requires
 * a WindowRecomposer on the window (Activity windows get one automatically,
 * but IME windows do not).
 *
 * AbstractComposeView has a fallback: if no Recomposer is found in the view tree
 * AND the view implements CoroutineScope, it creates an independent Recomposer.
 * This class does exactly that.
 */
class KeyboardComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr),
    LifecycleOwner,
    SavedStateRegistryOwner,
    CoroutineScope {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val supervisorJob = SupervisorJob()

    override val coroutineContext = Dispatchers.Main.immediate + supervisorJob
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var content: @Composable () -> Unit = {}

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        setViewTreeLifecycleOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
    }

    fun setContent(content: @Composable () -> Unit) {
        this.content = content
    }

    @Composable
    override fun Content() {
        content()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onDetachedFromWindow()
    }

    fun disposeKeyboard() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        supervisorJob.cancel()
        disposeComposition()
    }
}
