package com.horizon.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Horizon Voice — Monochrome Beat
 * Exact Jetpack Compose translation of the HTML UI.
 *
 * Palette: Gray (#121212), White (#FFFFFF)
 * Design: Micro buttons, Box animation, Thin borders.
 */

// ─── Language State ───────────────────────────────────────────────

enum class VoiceLang(val code: String, val label: String) {
    ENGLISH("en-US", "English"),
    BANGLA("bn-BD", "Bangla")
}

// ─── Data class for beat bar animation params ─────────────────────

data class BeatBarParams(
    val durationMs: Long,
    val delayMs: Long
)

// ─── Main Voice Typing Bar ────────────────────────────────────────

@Composable
fun VoiceTypingBar(
    onTextRecognized: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var currentLang by remember { mutableStateOf(VoiceLang.ENGLISH) }
    var partialText by remember { mutableStateOf("") }

    // SpeechRecognizer reference
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Build the recognition intent
    fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLang.code)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLang.code)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    // Set up recognition listener
    fun startListening() {
        speechRecognizer?.let { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    partialText = ""
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }
                override fun onError(error: Int) {
                    isListening = false
                    partialText = ""
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) {
                        onTextRecognized(text)
                    }
                    partialText = ""
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialText = matches?.firstOrNull() ?: ""
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(buildIntent())
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun toggleAction() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun toggleLang() {
        currentLang = if (currentLang == VoiceLang.ENGLISH) VoiceLang.BANGLA else VoiceLang.ENGLISH
        // If currently listening, restart with new language
        if (isListening) {
            stopListening()
            // Small delay then restart
            speechRecognizer?.let {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 200)
            }
        }
    }

    // ─── UI ────────────────────────────────────────────────────────

    val bgColor = Color(0xFF121212)
    val white = Color.White
    val white20 = Color(0x33FFFFFF)  // white/20
    val white40 = Color(0x66FFFFFF)  // white/40
    val white80 = Color(0xCCFFFFFF)  // white/80

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        // Partial text display (when recognizing)
        if (partialText.isNotEmpty()) {
            Text(
                text = partialText,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                maxLines = 1
            )
        }

        // Voice bar — exact replica of the HTML toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(bgColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Language Button (micro, rounded-full, hairline border) ──
            MicroButton(
                label = currentLang.label,
                borderColor = white20,
                textColor = white40,
                onClick = { toggleLang() }
            )

            // ── CENTER: Box Beat Visualizer ──
            BoxBeatVisualizer(
                isListening = isListening,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            )

            // ── Action Button (Start/Stop) ──
            MicroButton(
                label = if (isListening) "Stop" else "Start",
                borderColor = if (isListening) white80 else white40,
                textColor = white,
                minWidth = 65.dp,
                onClick = { toggleAction() }
            )
        }

        // Close row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .clickable { onClose() }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "▲ Hide Voice",
                color = Color(0xFF636366),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Micro Button (matches HTML micro buttons) ────────────────────

@Composable
fun MicroButton(
    label: String,
    borderColor: Color,
    textColor: Color,
    minWidth: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = minWidth)
            .height(24.dp)
            .clip(RoundedCornerShape(100f))  // rounded-full
            .border(0.5.dp, borderColor, RoundedCornerShape(100f))  // border-hairline
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,  // tracking-widest
            textAlign = TextAlign.Center
        )
    }
}

// ─── Box Beat Visualizer (exact HTML animation) ───────────────────

@Composable
fun BoxBeatVisualizer(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    // Generate 20 bars with random durations and delays (matches HTML)
    val bars = remember {
        List(20) {
            BeatBarParams(
                durationMs = (350 + Random.nextFloat() * 450).toLong(),
                delayMs = (Random.nextFloat() * 500).toLong()
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),  // gap-[4px]
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { params ->
            BoxBeatBar(
                isListening = isListening,
                durationMs = params.durationMs,
                delayMs = params.delayMs
            )
        }
    }
}

@Composable
fun BoxBeatBar(
    isListening: Boolean,
    durationMs: Long,
    delayMs: Long
) {
    // Animate scaleY when listening
    val infiniteTransition = rememberInfiniteTransition(label = "beat")

    val scale by if (isListening) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = durationMs.toInt()
                    0.1f at 0                           // 0%
                    0.1f at (durationMs * 0.15).toInt()  // 15%
                    1.0f at (durationMs * 0.45).toInt()  // 45%
                    0.5f at (durationMs * 0.55).toInt()  // 55%
                    0.1f at (durationMs * 0.85).toInt()  // 85%
                    0.1f at durationMs.toInt()            // 100%
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMs.toInt())
            ),
            label = "beatScale"
        )
    } else {
        remember { mutableStateOf(0.1f) }
    }

    // Opacity mirrors the scale pattern from CSS
    val barAlpha = if (isListening) {
        0.15f + (scale - 0.1f) * (0.9f - 0.15f) / (1.0f - 0.1f)
    } else {
        0.15f
    }

    Box(
        modifier = Modifier
            .width(6.dp)   // width: 6px
            .height(22.dp) // height: 22px
            .graphicsLayer {
                scaleY = scale
                alpha = barAlpha
            }
            .background(Color.White)  // strictly white, sharp box type
    )
}
