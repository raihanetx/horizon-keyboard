package com.horizon.keyboard.ui.setup

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.horizon.keyboard.data.SecureKeyStore

/**
 * Standalone Activity for editing the Groq API key.
 */
class ApiKeyActivity : Activity() {

    private lateinit var groqKeyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Title
        content.addView(TextView(this).apply {
            text = "API Key Settings"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })

        content.addView(TextView(this).apply {
            text = "Key is encrypted with Android Keystore (AES-256-GCM)"
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, dp(24))
        })

        // Groq API Key
        content.addView(labelText("GROQ API KEY (WHISPER)"))
        content.addView(TextView(this).apply {
            text = "Free: 2,000 requests/day · English specialist"
            setTextColor(Color.parseColor("#636366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(dp(4), 0, 0, dp(8))
        })
        groqKeyInput = editText("Enter Groq API key...")
        content.addView(groqKeyInput)

        scrollView.addView(content)
        root.addView(scrollView)

        // Buttons
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        buttonRow.addView(button("Cancel", "#636366") { finish() })
        buttonRow.addView(button("Save Key", "#34C759") { saveKey() })
        root.addView(buttonRow)

        setContentView(root)

        // Load existing key
        loadKey()

        groqKeyInput.requestFocus()
        groqKeyInput.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(groqKeyInput, InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }

    private fun loadKey() {
        try {
            val groqKey = SecureKeyStore.getGroqKey(this)
            if (groqKey.isNotEmpty()) {
                groqKeyInput.setText(groqKey)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not load existing key: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveKey() {
        val groqKey = groqKeyInput.text.toString().trim()

        if (groqKey.isEmpty()) {
            Toast.makeText(this, "Enter your Groq API key", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            SecureKeyStore.setGroqKey(this, groqKey)
            Toast.makeText(this, "✓ Key saved!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            try {
                getSharedPreferences("horizon_secure_keys", MODE_PRIVATE).edit().clear().apply()
                SecureKeyStore.setGroqKey(this, groqKey)
                Toast.makeText(this, "✓ Key saved! (storage reset)", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e2: Exception) {
                Toast.makeText(this, "Failed to save: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun labelText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#FF9F0A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
        }
    }

    private fun editText(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#636366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setBackgroundResource(0)
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                bottomMargin = dp(4)
            }
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2E"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#3A3A3C"))
            }
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

    private fun button(text: String, colorHex: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
