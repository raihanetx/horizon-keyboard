package com.horizon.keyboard.ui.screen

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horizon.keyboard.ui.clipboard.ClipboardPanel
import com.horizon.keyboard.ui.keyboard.KeyboardContainer
import com.horizon.keyboard.ui.keyboard.ToolbarAction
import com.horizon.keyboard.ui.keyboard.ToolbarMode
import com.horizon.keyboard.ui.theme.DarkBackground
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.LightBackground
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.viewmodel.ClipboardViewModel
import com.horizon.keyboard.viewmodel.KeyboardViewModel
import com.horizon.keyboard.viewmodel.VoiceViewModel

@Composable
fun MainScreen(
    keyboardViewModel: KeyboardViewModel = viewModel(),
    voiceViewModel: VoiceViewModel = viewModel(),
    clipboardViewModel: ClipboardViewModel = viewModel()
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val voiceState by voiceViewModel.uiState.collectAsState()
    val clipboardState by clipboardViewModel.uiState.collectAsState()

    var textContent by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            voiceViewModel.startListening(context)
        }
    }

    DisposableEffect(Unit) {
        keyboardViewModel.onKeyPress = { key ->
            textContent += key
            if (voiceState.isListening) {
                voiceViewModel.stopListening()
            }
        }
        keyboardViewModel.onBackspace = {
            if (textContent.isNotEmpty()) {
                textContent = textContent.dropLast(1)
            }
        }
        voiceViewModel.onResult = { text ->
            textContent = if (textContent.isEmpty()) text else "$textContent $text"
        }

        onDispose {
            voiceViewModel.cleanup()
        }
    }

    val toolbarMode = when {
        voiceState.isListening -> ToolbarMode.VOICE
        clipboardState.isPanelOpen -> ToolbarMode.DEFAULT
        textContent.isNotEmpty() -> ToolbarMode.TYPING
        else -> ToolbarMode.DEFAULT
    }

    val suggestions = remember(textContent) {
        keyboardViewModel.getSuggestions(textContent)
    }

    val bottomPadding = if (clipboardState.isPanelOpen) 560.dp else 280.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
    ) {
        TextInputSection(
            textContent = textContent,
            onTextChange = { textContent = it },
            isVoiceListening = voiceState.isListening,
            voiceLanguage = voiceState.currentLanguage.displayName,
            isDark = isDark,
            bottomPadding = bottomPadding
        )

        // Keyboard + Clipboard stacked, anchored to bottom
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            KeyboardContainer(
                toolbarMode = toolbarMode,
                onToolbarAction = { action ->
                    handleToolbarAction(
                        action = action,
                        hasPermission = hasPermission,
                        context = context,
                        voiceViewModel = voiceViewModel,
                        clipboardViewModel = clipboardViewModel,
                        permissionLauncher = permissionLauncher,
                        onCopy = { clipboardViewModel.copy(textContent) }
                    )
                },
                voiceLanguage = voiceState.currentLanguage.displayName,
                suggestions = suggestions,
                onSuggestionClick = { word ->
                    textContent = keyboardViewModel.getSuggestionReplacement(textContent, word)
                },
                keyboardRows = keyboardViewModel.getLayout(),
                isShiftActive = keyboardViewModel.uiState.isShift,
                onKeyClick = { keyboardViewModel.handleKeyPress(it) }
            )

            if (clipboardState.isPanelOpen) {
                ClipboardPanel(
                    recentItems = clipboardViewModel.getRecentItems(),
                    pinnedItems = clipboardViewModel.getPinnedItems(),
                    toastMessage = clipboardState.toastMessage,
                    onToastShown = { clipboardViewModel.clearToast() },
                    onCopy = { text ->
                        textContent = if (textContent.isEmpty()) text else "$textContent $text"
                        clipboardViewModel.closePanel()
                    },
                    onTogglePin = { clipboardViewModel.togglePin(it) }
                )
            }
        }
    }
}

@Composable
private fun TextInputSection(
    textContent: String,
    onTextChange: (String) -> Unit,
    isVoiceListening: Boolean,
    voiceLanguage: String,
    isDark: Boolean,
    bottomPadding: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Horizon Keyboard",
            fontSize = 24.sp,
            color = if (isDark) DarkKeyText else LightKeyText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = textContent,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = {
                Text(
                    text = getPlaceholder(isVoiceListening, voiceLanguage),
                    color = if (isDark) DarkKeyText.copy(alpha = 0.4f) else LightKeyText.copy(alpha = 0.4f)
                )
            },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = if (isDark) DarkKeyText else LightKeyText
            ),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isDark) Color(0xFF2D3748) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF2D3748) else Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

private fun getPlaceholder(isVoiceListening: Boolean, voiceLanguage: String): String {
    return when {
        isVoiceListening && voiceLanguage == "বাংলা" -> "বাংলায় বলুন..."
        isVoiceListening -> "Speak in English..."
        else -> "Start typing..."
    }
}

private fun handleToolbarAction(
    action: ToolbarAction,
    hasPermission: Boolean,
    context: Context,
    voiceViewModel: VoiceViewModel,
    clipboardViewModel: ClipboardViewModel,
    permissionLauncher: ActivityResultLauncher<String>,
    onCopy: () -> Unit
) {
    when (action) {
        ToolbarAction.VOICE_TYPING -> {
            if (hasPermission) {
                voiceViewModel.startListening(context)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        ToolbarAction.STOP_VOICE -> voiceViewModel.stopListening()
        ToolbarAction.EN_BN_TOGGLE -> voiceViewModel.toggleLanguage()
        ToolbarAction.CLIPBOARD -> {
            clipboardViewModel.togglePanel()
        }
        ToolbarAction.SETTINGS -> {
            onCopy()
        }
        else -> {}
    }
}
