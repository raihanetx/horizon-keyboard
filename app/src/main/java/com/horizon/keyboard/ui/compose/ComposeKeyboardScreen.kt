package com.horizon.keyboard.ui.compose

/**
 * EXPERIMENTAL — Compose-based keyboard UI.
 *
 * This is an alternative implementation using Jetpack Compose.
 * It is NOT used by the IME service ([com.horizon.keyboard.HorizonKeyboardService]).
 * The actual keyboard uses the View-based system in [com.horizon.keyboard.KeyboardView].
 *
 * Kept as a reference/preview for potential future Compose migration.
 */

import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Colors ---
private val ColorBg = Color(0xFF1C1C1E)
private val ColorKb = Color(0xFF2C2C2E)
private val ColorText = Color(0xFFFFFFFF)
private val ColorTextMuted = Color(0xFFA0A0A8)
private val ColorTextDark = Color(0xFF636366)
private val ColorAccent = Color(0xFF0A84FF)
private val ColorKeyShadow = Color(0xFF151517)
private val ColorKeySurfaceDark = Color(0xFF3A3A3C)
private val ColorKeySurfaceLight = Color(0xFF2C2C2E)
private val ColorKeySpecial = Color(0xFF48484A)
private val ColorRed = Color(0xFFFF453A)

enum class AppTab { Keyboard, Translate, Clipboard, Terminal, Settings }

// ─── Main Keyboard UI (exact clone of ui reference) ─────────────

@Composable
fun HorizonKeyboardUI(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onShift: () -> Unit,
    onSpace: () -> Unit,
    onSymbol: () -> Unit
) {
    var shift by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(AppTab.Keyboard) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(308.dp)
            .background(ColorBg)
    ) {
        // Suggestion Bar
        SuggestionBar(onInsert = { word -> onKeyPress("$word ") })

        // Toolbar Area — icon toolbar (voice typing disabled)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(ColorKb)
                .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(0.dp))
        ) {
            Toolbar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }

        // Main Keyboard Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(ColorBg)
                .padding(top = 8.dp, start = 6.dp, end = 6.dp)
        ) {
            when (currentTab) {
                AppTab.Keyboard -> KeyboardLayout(
                    isShift = shift,
                    onShiftToggle = { shift = !shift },
                    onKeyPress = { key ->
                        when (key) {
                            "⌫" -> onBackspace()
                            "SPACE" -> onSpace()
                            "DONE" -> {
                                onEnter()
                                currentTab = AppTab.Keyboard
                            }
                            else -> {
                                val output = if (shift) key.uppercase() else key.lowercase()
                                onKeyPress(output)
                                if (shift) shift = false
                            }
                        }
                    }
                )
                AppTab.Translate -> TranslatePanel("")
                AppTab.Clipboard -> ClipboardPanel()
                AppTab.Terminal -> TerminalPanel()
                AppTab.Settings -> SettingsPanel()
            }
        }
    }
}

// ─── Suggestion Bar ──────────────────────────────────────────────

@Composable
fun SuggestionBar(onInsert: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ColorBg)
            .border(1.dp, ColorKb),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val words = listOf("Hello", "The", "Thanks")
        words.forEachIndexed { index, word ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onInsert(word) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = word, color = ColorTextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (index < words.size - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .background(Color(0xFF3A3A3C))
                    )
                }
            }
        }
    }
}

// ─── Toolbar ─────────────────────────────────────────────────────

@Composable
fun Toolbar(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icons = listOf(
            AppTab.Keyboard to "M20 5H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z",
            AppTab.Translate to "M12.87 15.07l-2.54-2.51.03-.03A17.52 17.52 0 0014.07 6H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z",
            AppTab.Clipboard to "M19 2h-4.18C14.4.84 13.3 0 12 0S9.6.84 9.18 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z",
            AppTab.Terminal to "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z",
            AppTab.Settings to "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 00.12-.61l-1.92-3.32a.488.488 0 00-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 00-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 00-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24-1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"
        )

        icons.forEach { (tab, pathData) ->
            val isActive = currentTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                SvgIcon(pathData = pathData, color = if (isActive) ColorAccent else ColorTextMuted)
            }
        }
    }
}

@Composable
fun SvgIcon(pathData: String, color: Color) {
    val path = remember {
        PathParser().parsePathString(pathData).toPath()
    }
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.width / 24f
        withTransform({
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawPath(path = path, color = color)
        }
    }
}

// ─── Keyboard Layout ─────────────────────────────────────────────

@Composable
fun KeyboardLayout(isShift: Boolean, onShiftToggle: () -> Unit, onKeyPress: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        KeyRow {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach {
                KeyboardKey(text = if (isShift) it.uppercase() else it, onClick = { onKeyPress(it) })
            }
        }
        KeyRow {
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach {
                KeyboardKey(text = if (isShift) it.uppercase() else it, onClick = { onKeyPress(it) })
            }
        }
        KeyRow {
            KeyboardKey(text = "⇧", weight = 1.4f, bg = if (isShift) ColorAccent else ColorKeySpecial, onClick = onShiftToggle)
            listOf("z", "x", "c", "v", "b", "n", "m").forEach {
                KeyboardKey(text = if (isShift) it.uppercase() else it, onClick = { onKeyPress(it) })
            }
            KeyboardKey(text = "⌫", weight = 1.4f, bg = ColorKeySpecial, onClick = { onKeyPress("⌫") })
        }
        KeyRow {
            KeyboardKey(text = "123", weight = 1.4f, bg = ColorKeySpecial, fontSize = 11.sp, onClick = {})
            KeyboardKey(text = "@", weight = 1f, onClick = { onKeyPress("@") })
            KeyboardKey(text = "SPACE", weight = 5f, fontSize = 11.sp, letterSpacing = 2.sp, onClick = { onKeyPress("SPACE") })
            KeyboardKey(text = ".", weight = 1f, onClick = { onKeyPress(".") })
            KeyboardKey(text = "DONE", weight = 2f, bg = ColorAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, onClick = { onKeyPress("DONE") })
        }
    }
}

@Composable
fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun RowScope.KeyboardKey(
    text: String,
    weight: Float = 1f,
    bg: Color? = null,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(46.dp)
            .background(ColorKeyShadow, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) onClick()
                    }
                )
            }
    ) {
        val backgroundModifier = if (bg != null) {
            Modifier.background(if (isPressed) ColorKeySpecial else bg, RoundedCornerShape(8.dp))
        } else {
            Modifier.background(
                brush = if (isPressed) SolidColor(ColorKeySpecial) else Brush.verticalGradient(listOf(ColorKeySurfaceDark, ColorKeySurfaceLight)),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 2.dp)
                .offset(y = if (isPressed) 2.dp else 0.dp)
                .then(backgroundModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = ColorText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                letterSpacing = letterSpacing,
                fontFamily = if (text.length > 1) FontFamily.SansSerif else FontFamily.Default,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
    }
}

// ─── Panels ──────────────────────────────────────────────────────

@Composable
fun PanelBox(label: String, text: String, borderColor: Color = Color(0xFF3A3A3C), labelColor: Color = ColorTextDark) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(ColorKb, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            color = labelColor,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = text.ifEmpty { "..." },
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TranslatePanel(sourceText: String) {
    var inputText by remember { mutableStateOf(sourceText) }
    var translatedText by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("en-bn") } // en-bn or bn-en

    // Simple dictionary for demo translations
    val enToBn = remember {
        mapOf(
            "hello" to "হ্যালো", "hi" to "হাই", "how are you" to "কেমন আছেন",
            "thank you" to "ধন্যবাদ", "thanks" to "ধন্যবাদ", "good" to "ভালো",
            "bad" to "খারাপ", "yes" to "হ্যাঁ", "no" to "না",
            "please" to "দয়া করে", "sorry" to "দুঃখিত", "help" to "সাহায্য",
            "what" to "কি", "where" to "কোথায়", "when" to "কখন",
            "why" to "কেন", "who" to "কে", "how" to "কিভাবে",
            "i" to "আমি", "you" to "তুমি", "he" to "সে", "she" to "সে",
            "we" to "আমরা", "they" to "তারা", "am" to "আছি", "is" to "আছে",
            "are" to "আছ", "was" to "ছিল", "were" to "ছিল",
            "love" to "ভালোবাসা", "like" to "পছন্দ", "want" to "চাই",
            "need" to "প্রয়োজন", "eat" to "খাওয়া", "drink" to "পানীয়",
            "go" to "যাওয়া", "come" to "আসা", "see" to "দেখা",
            "hear" to "শোনা", "speak" to "বলা", "write" to "লেখা",
            "read" to "পড়া", "work" to "কাজ", "home" to "বাড়ি",
            "school" to "স্কুল", "water" to "পানি", "food" to "খাবার",
            "friend" to "বন্ধু", "family" to "পরিবার", "mother" to "মা",
            "father" to "বাবা", "brother" to "ভাই", "sister" to "বোন",
            "today" to "আজ", "tomorrow" to "কাল", "yesterday" to "গতকাল",
            "morning" to "সকাল", "night" to "রাত", "day" to "দিন",
            "time" to "সময়", "name" to "নাম", "man" to "মানুষ",
            "woman" to "মহিলা", "child" to "শিশু", "big" to "বড়",
            "small" to "ছোট", "new" to "নতুন", "old" to "পুরানো",
            "happy" to "সুখী", "sad" to "দুঃখী", "beautiful" to "সুন্দর",
            "one" to "এক", "two" to "দুই", "three" to "তিন",
            "four" to "চার", "five" to "পাঁচ", "six" to "ছয়",
            "seven" to "সাত", "eight" to "আট", "nine" to "নয়", "ten" to "দশ"
        )
    }

    val bnToEn = remember { enToBn.entries.associate { (k, v) -> v to k } }

    fun translate(text: String): String {
        if (text.isBlank()) return ""
        val dict = if (direction == "en-bn") enToBn else bnToEn
        // Try exact match first
        dict[text.trim().lowercase()]?.let { return it }
        // Try word-by-word
        val words = text.trim().split("\\s+".toRegex())
        val result = words.joinToString(" ") { word ->
            dict[word.lowercase()] ?: word
        }
        return result
    }

    LaunchedEffect(inputText, direction) {
        translatedText = translate(inputText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Direction toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fromLang = if (direction == "en-bn") "English" else "বাংলা"
            val toLang = if (direction == "en-bn") "বাংলা" else "English"
            Text(
                text = "$fromLang → $toLang",
                color = ColorAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    direction = if (direction == "en-bn") "bn-en" else "en-bn"
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "⇄",
                color = ColorTextMuted,
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    direction = if (direction == "en-bn") "bn-en" else "en-bn"
                    inputText = translatedText
                }
            )
        }

        // Input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.fillMaxSize(),
                cursorBrush = SolidColor(ColorAccent)
            )
            if (inputText.isEmpty()) {
                Text(
                    text = "Type text to translate...",
                    color = ColorTextDark,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Result area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                .border(1.dp, ColorAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                text = translatedText.ifEmpty { "Translation appears here..." },
                color = if (translatedText.isEmpty()) ColorTextDark else Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ClipboardPanel() {
    val context = LocalContext.current
    var clipboardText by remember { mutableStateOf("") }
    var clipHistory by remember { mutableStateOf(mutableListOf<String>()) }

    // Read clipboard on first composition
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                clipboardText = text
                if (clipHistory.isEmpty() || clipHistory.first() != text) {
                    val newHistory = mutableListOf(text)
                    newHistory.addAll(clipHistory.filter { it != text })
                    clipHistory = newHistory.take(20).toMutableList()
                }
            }
        }
    }

    // Listen for clipboard changes
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    clipboardText = text
                    // Add to history (deduplicate, most recent first)
                    val newHistory = mutableListOf(text)
                    newHistory.addAll(clipHistory.filter { it != text })
                    clipHistory = newHistory.take(20).toMutableList()
                }
            }
        }
        cm.addPrimaryClipChangedListener(listener)
        onDispose {
            cm.removePrimaryClipChangedListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Current clipboard
        PanelBox(
            label = "Current Clipboard",
            text = clipboardText.ifEmpty { "No text in clipboard" },
            borderColor = ColorAccent,
            labelColor = ColorAccent
        )

        Spacer(modifier = Modifier.height(8.dp))

        // History header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLIP HISTORY",
                fontSize = 9.sp,
                color = ColorTextDark,
                fontWeight = FontWeight.ExtraBold
            )
            if (clipHistory.isNotEmpty()) {
                Text(
                    text = "Clear",
                    fontSize = 10.sp,
                    color = ColorRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        clipHistory = mutableListOf()
                        clipboardText = ""
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Clip history list
        if (clipHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No clips yet.\nCopy text to start tracking.",
                    color = ColorTextDark,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(clipHistory.size) { index ->
                    val clip = clipHistory[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(6.dp))
                            .clickable {
                                // Copy to clipboard
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("horizon", clip))
                                clipboardText = clip
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clip,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy",
                            color = ColorAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalPanel() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("MiMo Dev Terminal Ready...", color = ColorTextDark, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$", color = Color(0xFF32D74B), fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 8.dp))
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(color = ColorText, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    Box {
                        Text("Command...", color = ColorTextDark, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsPanel() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        PanelBox(label = "System", text = "Haptics: Active")
    }
}
