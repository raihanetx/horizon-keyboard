package com.horizon.keyboard.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horizon.keyboard.ui.keyboard.KeyboardContainer
import com.horizon.keyboard.ui.keyboard.ToolbarAction
import com.horizon.keyboard.ui.keyboard.ToolbarMode
import com.horizon.keyboard.ui.theme.DarkBackground
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.Dimens
import com.horizon.keyboard.ui.theme.LightBackground
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.viewmodel.KeyboardViewModel
import com.horizon.keyboard.viewmodel.VoiceViewModel

/**
 * Main screen of the keyboard application.
 */
@Composable
fun MainScreen(
    keyboardViewModel: KeyboardViewModel = viewModel(),
    voiceViewModel: VoiceViewModel = viewModel()
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val voiceState by voiceViewModel.uiState.collectAsState()
    
    var textContent by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            voiceViewModel.startListening(context)
        }
    }
    
    // Setup callbacks
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
    
    // Determine toolbar mode
    val toolbarMode = when {
        voiceState.isListening -> ToolbarMode.VOICE
        textContent.isNotEmpty() -> ToolbarMode.TYPING
        else -> ToolbarMode.DEFAULT
    }
    
    // Get suggestions
    val suggestions = remember(textContent) {
        keyboardViewModel.getSuggestions(textContent)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
    ) {
        // Text input area
        TextInputSection(
            textContent = textContent,
            onTextChange = { textContent = it },
            isVoiceListening = voiceState.isListening,
            voiceLanguage = voiceState.currentLanguage.displayName,
            isDark = isDark
        )
        
        // Keyboard
        KeyboardContainer(
            toolbarMode = toolbarMode,
            onToolbarAction = { action ->
                handleToolbarAction(
                    action = action,
                    hasPermission = hasPermission,
                    context = context,
                    voiceViewModel = voiceViewModel,
                    permissionLauncher = permissionLauncher
                )
            },
            voiceLanguage = voiceState.currentLanguage.displayName,
            suggestions = suggestions,
            onSuggestionClick = { word ->
                textContent = keyboardViewModel.getSuggestionReplacement(textContent, word)
            },
            keyboardRows = keyboardViewModel.getLayout(),
            isShiftActive = keyboardViewModel.uiState.isShift,
            onKeyClick = { keyboardViewModel.handleKeyPress(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TextInputSection(
    textContent: String,
    onTextChange: (String) -> Unit,
    isVoiceListening: Boolean,
    voiceLanguage: String,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpacingLg)
            .padding(bottom = Dimens.KeyboardHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Horizon Keyboard",
            fontSize = Dimens.TextTitle,
            color = if (isDark) DarkKeyText else LightKeyText,
            modifier = Modifier.padding(bottom = Dimens.SpacingLg)
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
                fontSize = Dimens.TextLarge,
                color = if (isDark) DarkKeyText else LightKeyText
            ),
            shape = RoundedCornerShape(Dimens.RadiusLg),
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
    context: android.content.Context,
    voiceViewModel: VoiceViewModel,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
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
        else -> {} // Other actions not implemented yet
    }
}
