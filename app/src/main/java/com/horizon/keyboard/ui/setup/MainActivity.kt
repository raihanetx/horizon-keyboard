package com.horizon.keyboard.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.horizon.keyboard.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.horizon.keyboard.data.SecureKeyStore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorizonSetupScreen()
        }
    }
}

@Composable
fun HorizonSetupScreen() {
    val context = LocalContext.current
    val accent = Color(0xFF0A84FF)

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // Voice engine credential status
    var hasGroqKey by remember { mutableStateOf(SecureKeyStore.hasGroqKey(context)) }
    var hasGemmaKey by remember { mutableStateOf(SecureKeyStore.hasGemmaKey(context)) }
    val hasAnyKey = hasGroqKey || hasGemmaKey

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) {
            Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission denied — voice typing won't work", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "HORIZON KEYBOARD",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = Color(0xFF636366),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Keyboard icon (vector drawable)
        Image(
            painter = painterResource(id = R.drawable.ic_keyboard),
            contentDescription = "Keyboard",
            modifier = Modifier.size(64.dp),
            colorFilter = ColorFilter.tint(Color(0xFF0A84FF))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Horizon Keyboard",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A minimal keyboard with voice typing,\ntranslation & clipboard manager.",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Step 1: Microphone Permission
        Button(
            onClick = {
                if (micGranted) {
                    Toast.makeText(context, "Already granted!", Toast.LENGTH_SHORT).show()
                } else {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (micGranted) Color(0xFF34C759) else accent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_voice),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (micGranted) "✓  Microphone Permission Granted" else "Step 1: Grant Microphone Permission",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 2: Enable keyboard
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Step 2: Enable Horizon in Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 3: Switch keyboard
        OutlinedButton(
            onClick = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_globe),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Step 3: Switch to Horizon", color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 4: Voice Engine Credentials
        val apiKeyLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Refresh key status when returning from ApiKeyActivity
            hasGroqKey = SecureKeyStore.hasGroqKey(context)
            hasGemmaKey = SecureKeyStore.hasGemmaKey(context)
        }

        Button(
            onClick = {
                apiKeyLauncher.launch(Intent(context, ApiKeyActivity::class.java))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasAnyKey) Color(0xFF34C759) else Color(0xFFFF9F0A)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_voice),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasAnyKey) {
                    val parts = mutableListOf<String>()
                    if (hasGroqKey) parts.add("Whisper ✓")
                    if (hasGemmaKey) parts.add("Gemma ✓")
                    "Step 4: Voice Engine — ${parts.joinToString(" · ")}"
                } else {
                    "Step 4: Setup Voice Engine Credentials"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Credential safety indicator
        if (hasAnyKey) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "🔒 Credentials encrypted with Android Keystore (AES-256-GCM)",
                color = Color(0xFF34C759),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "1. Tap Step 1 → Allow microphone access\n" +
                    "2. Tap Step 2 → find \"Horizon Keyboard\" → toggle ON\n" +
                    "3. Tap Step 3 → select \"Horizon Keyboard\"\n" +
                    "4. Tap Step 4 → add API keys for voice typing",
            color = Color(0xFF636366),
            fontSize = 12.sp,
            textAlign = TextAlign.Start,
            lineHeight = 18.sp
        )
    }
}
