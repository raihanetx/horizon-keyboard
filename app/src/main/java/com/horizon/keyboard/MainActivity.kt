package com.horizon.keyboard

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horizon.keyboard.ui.components.KeyboardView
import com.horizon.keyboard.ui.components.ToolbarAction
import com.horizon.keyboard.ui.components.ToolbarMode
import com.horizon.keyboard.ui.theme.DarkBackground
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.HorizonKeyboardTheme
import com.horizon.keyboard.ui.theme.LightBackground
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.viewmodel.KeyboardViewModel
import com.horizon.keyboard.voice.VoiceRecognitionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorizonKeyboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KeyboardScreen()
                }
            }
        }
    }
}

@Composable
fun KeyboardScreen() {
    val viewModel: KeyboardViewModel = viewModel()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    var textContent by remember { mutableStateOf("") }
    var toolbarMode by remember { mutableStateOf(ToolbarMode.DEFAULT) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Voice recognition manager
    val voiceManager = remember { VoiceRecognitionManager(context) }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }
    
    // Set up voice callbacks
    voiceManager.onResult = { text ->
        textContent = if (textContent.isEmpty()) {
            text
        } else {
            "$textContent $text"
        }
        toolbarMode = if (textContent.isNotEmpty()) ToolbarMode.TYPING else ToolbarMode.DEFAULT
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            voiceManager.startListening()
            toolbarMode = ToolbarMode.VOICE
        }
    }

    viewModel.onKeyPress = { key ->
        textContent += key
        if (voiceManager.isListening) {
            voiceManager.stopListening()
        }
    }
    viewModel.onBackspace = {
        if (textContent.isNotEmpty()) {
            textContent = textContent.dropLast(1)
        }
    }

    val suggestions = remember(textContent) { viewModel.getSuggestions(textContent) }

    val effectiveToolbarMode = when {
        voiceManager.isListening -> ToolbarMode.VOICE
        textContent.isNotEmpty() -> ToolbarMode.TYPING
        else -> ToolbarMode.DEFAULT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 280.dp),
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
                onValueChange = { textContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        text = if (voiceManager.isListening) {
                            if (voiceManager.currentLanguage.displayName == "বাংলা") {
                                "বাংলায় বলুন..."
                            } else {
                                "Speak in English..."
                            }
                        } else {
                            "Start typing..."
                        },
                        color = if (isDark) DarkKeyText.copy(alpha = 0.4f) else LightKeyText.copy(alpha = 0.4f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = if (isDark) DarkKeyText else LightKeyText
                ),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFFFFFFF),
                    unfocusedContainerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFFFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        KeyboardView(
            viewModel = viewModel,
            suggestions = suggestions,
            onSuggestionClick = { word ->
                textContent = viewModel.getSuggestionReplacement(textContent, word)
            },
            toolbarMode = effectiveToolbarMode,
            onToolbarAction = { action ->
                when (action) {
                    ToolbarAction.VOICE_TYPING -> {
                        if (hasPermission) {
                            voiceManager.startListening()
                            toolbarMode = ToolbarMode.VOICE
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    ToolbarAction.STOP_VOICE -> {
                        voiceManager.stopListening()
                        toolbarMode = if (textContent.isNotEmpty()) ToolbarMode.TYPING else ToolbarMode.DEFAULT
                    }
                    ToolbarAction.EN_BN_TOGGLE -> {
                        // Toggle language
                        voiceManager.toggleLanguage()
                        // If currently listening, restart with new language
                        if (voiceManager.isListening) {
                            voiceManager.stopListening()
                            voiceManager.startListening()
                        }
                    }
                    else -> {}
                }
            },
            voiceLanguage = voiceManager.currentLanguage.displayName,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
