package com.horizon.keyboard

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Horizon Voice — Monochrome Beat
 * EXACT Jetpack Compose translation of the HTML file "voice typing ui"
 *
 * Line-by-line mapping from Tailwind classes → Compose modifiers.
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

// ─── Exact color palette from HTML ────────────────────────────────
// body { background: #000 }
private val ColorBody = Color(0xFF000000)
// bg-[#121212]
private val ColorToolbar = Color(0xFF121212)
// white
private val ColorWhite = Color.White
// white/10
private val ColorWhite10 = Color(0x1AFFFFFF)
// white/20
private val ColorWhite20 = Color(0x33FFFFFF)
// white/40
private val ColorWhite40 = Color(0x66FFFFFF)
// white/80
private val ColorWhite80 = Color(0xCCFFFFFF)

// ─── Main Voice Typing Bar ────────────────────────────────────────

@Composable
fun VoiceTypingBar(
    onTextRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var currentLang by remember { mutableStateOf(VoiceLang.ENGLISH) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLang.code)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLang.code)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    fun startListening() {
        speechRecognizer?.let { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) { isListening = false }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) onTextRecognized(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
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
        if (isListening) stopListening() else startListening()
    }

    fun toggleLang() {
        currentLang = if (currentLang == VoiceLang.ENGLISH) VoiceLang.BANGLA else VoiceLang.ENGLISH
        if (isListening) {
            stopListening()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startListening()
            }, 200)
        }
    }

    // ─── Single Line Toolbar — exact same height as header (48dp) ───
    // Matches: bg-[#121212], flex items-center, px-4, gap-3, border-t border-white/10
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // fills the 48dp toolbar container exactly
            .background(ColorToolbar)  // bg-[#121212]
            .border(0.5.dp, ColorWhite10)  // border-t border-white/10 (top only)
            .padding(horizontal = 16.dp),  // px-4
        verticalAlignment = Alignment.CenterVertically,  // items-center
        horizontalArrangement = Arrangement.spacedBy(12.dp)  // gap-3
    ) {
            // ─── HTML: <button id="langBtn" class="flex-shrink-0 h-6 px-3
            //           rounded-full border-hairline border-white/20 bg-transparent
            //           text-[8px] font-bold uppercase tracking-tighter
            //           text-white/40 active:scale-95 transition-all"> ───
            MicroButton(
                label = currentLang.label,
                fontSize = 8.sp,  // text-[8px]
                fontWeight = FontWeight.Bold,  // font-bold = 700
                letterSpacing = TextUnit(-0.05f, TextUnitType.Em),  // tracking-tighter = -0.05em
                textColor = ColorWhite40,  // text-white/40
                borderColor = ColorWhite20,  // border-white/20
                paddingHorizontal = 12.dp,  // px-3
                height = 24.dp,  // h-6
                onClick = { toggleLang() }
            )

            // ─── HTML: <div id="beatContainer" class="flex-1 flex items-center
            //           justify-center gap-[4px] h-8 overflow-hidden"> ───
            BoxBeatVisualizer(
                isListening = isListening,
                modifier = Modifier
                    .weight(1f)  // flex-1
                    .height(32.dp)  // h-8
            )

            // ─── HTML: <button id="actionBtn" class="flex-shrink-0 h-6 min-w-[65px]
            //           px-2 rounded-full border-hairline border-white/40 bg-transparent
            //           text-[8px] font-black uppercase tracking-widest
            //           text-white active:scale-95 transition-all"> ───
            MicroButton(
                label = if (isListening) "Stop" else "Start",
                fontSize = 8.sp,  // text-[8px]
                fontWeight = FontWeight.Black,  // font-black = 900
                letterSpacing = TextUnit(0.1f, TextUnitType.Em),  // tracking-widest = 0.1em
                textColor = ColorWhite,  // text-white
                borderColor = if (isListening) ColorWhite80 else ColorWhite40,  // border-white/40 → white/80
                paddingHorizontal = 8.dp,  // px-2
                minWidth = 65.dp,  // min-w-[65px]
                height = 24.dp,  // h-6
                onClick = { toggleAction() }
            )
        }
}

// ─── Micro Button ─────────────────────────────────────────────────
// Matches HTML: rounded-full, border-hairline (0.5dp), bg-transparent, text-[8px]

@Composable
private fun MicroButton(
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    textColor: Color,
    borderColor: Color,
    paddingHorizontal: androidx.compose.ui.unit.Dp = 12.dp,
    minWidth: androidx.compose.ui.unit.Dp = 0.dp,
    height: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = minWidth)
            .height(height)  // h-6
            .clip(RoundedCornerShape(100f))  // rounded-full
            .border(0.5.dp, borderColor, RoundedCornerShape(100f))  // border-hairline
            .clickable(
                interactionSource = interactionSource,
                indication = null  // no ripple (bg-transparent)
            ) { onClick() }
            .padding(horizontal = paddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),  // uppercase
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Box Beat Visualizer ──────────────────────────────────────────
// HTML: 20 bars, gap-[4px], h-8, overflow-hidden

@Composable
private fun BoxBeatVisualizer(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val bars = remember {
        List(20) {  // totalBars = 20
            BeatBarParams(
                durationMs = (350 + Random.nextFloat() * 450).toLong(),  // 0.35 + Math.random() * 0.45
                delayMs = (Random.nextFloat() * 500).toLong()  // Math.random() * 0.5
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),  // gap-[4px]
        verticalAlignment = Alignment.CenterVertically  // items-center
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

// ─── Single Beat Bar ──────────────────────────────────────────────
// HTML .beat-bar: width: 6px, height: 22px, background-color: #ffffff,
//                 border-radius: 0px, transform: scaleY(0.1), transform-origin: center
// HTML @keyframes box-beat:
//   0%, 15%  → scaleY(0.1), opacity 0.15
//   45%      → scaleY(1),   opacity 0.9
//   55%      → scaleY(0.5), opacity 0.5
//   85%, 100%→ scaleY(0.1), opacity 0.15
// CSS easing: cubic-bezier(.2, .9, .3, 1)

@Composable
private fun BoxBeatBar(
    isListening: Boolean,
    durationMs: Long,
    delayMs: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beat")

    val scale by if (isListening) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = durationMs.toInt()
                    // 0%  → scaleY(0.1), opacity 0.15
                    0.1f at 0
                    // 15% → scaleY(0.1), opacity 0.15
                    0.1f at (durationMs * 0.15).toInt()
                    // 45% → scaleY(1.0), opacity 0.9
                    1.0f at (durationMs * 0.45).toInt()
                    // 55% → scaleY(0.5), opacity 0.5
                    0.5f at (durationMs * 0.55).toInt()
                    // 85% → scaleY(0.1), opacity 0.15
                    0.1f at (durationMs * 0.85).toInt()
                    // 100% → scaleY(0.1), opacity 0.15
                    0.1f at durationMs.toInt()
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMs.toInt())  // animationDelay
            ),
            label = "beatScale"
        )
    } else {
        remember { mutableStateOf(0.1f) }  // resting: scaleY(0.1)
    }

    // Opacity: CSS maps 0.1→0.15, 1.0→0.9 linearly
    val barAlpha = if (isListening) {
        0.15f + (scale - 0.1f) * (0.9f - 0.15f) / (1.0f - 0.1f)
    } else {
        0.15f  // resting opacity
    }

    // ─── HTML: .beat-bar { width: 6px; height: 22px; background-color: #ffffff;
    //           border-radius: 0px; transform: scaleY(0.1); transform-origin: center; } ───
    Box(
        modifier = Modifier
            .width(6.dp)  // width: 6px
            .height(22.dp)  // height: 22px
            .graphicsLayer {
                scaleY = scale  // transform: scaleY
                alpha = barAlpha  // opacity
                // transform-origin: center (default in graphicsLayer)
            }
            .background(ColorWhite)  // background-color: #ffffff, border-radius: 0px (sharp)
    )
}
