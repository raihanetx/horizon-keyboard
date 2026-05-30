package com.horizon.keyboard.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class VoiceLanguage(val locale: Locale, val displayName: String, val prompt: String) {
    ENGLISH(Locale.US, "EN", "Speak in English..."),
    BANGLA(Locale("bn", "BD"), "বাং", "বাংলায় বলুন...")
}

class VoiceRecognitionManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val TAG = "VoiceRecognition"
    
    var isListening by mutableStateOf(false)
        private set
    
    var recognizedText by mutableStateOf("")
        private set
    
    var currentLanguage by mutableStateOf(VoiceLanguage.ENGLISH)
        private set
    
    var onResult: ((String) -> Unit)? = null
    
    fun toggleLanguage() {
        // Stop current listening if active
        stopListening()
        
        // Switch language
        currentLanguage = if (currentLanguage == VoiceLanguage.ENGLISH) {
            VoiceLanguage.BANGLA
        } else {
            VoiceLanguage.ENGLISH
        }
        Log.d(TAG, "Language switched to: ${currentLanguage.displayName} (locale: ${currentLanguage.locale})")
    }
    
    fun startListening() {
        recognizedText = ""
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        
        try {
            // Destroy previous instance completely
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            
            // Create fresh instance
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech. Language: ${currentLanguage.displayName} (${currentLanguage.locale})")
                    isListening = true
                }
                
                override fun onBeginningOfSpeech() {
                    isListening = true
                }
                
                override fun onRmsChanged(rmsdB: Float) {}
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                    isListening = false
                }
                
                override fun onError(error: Int) {
                    isListening = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        else -> "Error: $error"
                    }
                    Log.e(TAG, "Recognition error: $errorMsg")
                }
                
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizedText = text
                    Log.d(TAG, "Result: '$text' (expected language: ${currentLanguage.displayName})")
                    if (text.isNotEmpty()) {
                        onResult?.invoke(text)
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizedText = text
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            // Create intent with EXPLICIT language settings
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                // Required: Language model
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                
                // CRITICAL: Set the language explicitly using locale string
                val languageCode = currentLanguage.locale.toString()
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                
                // Partial results for real-time feedback
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                
                // Max results
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                
                // Prompt for the user
                putExtra(RecognizerIntent.EXTRA_PROMPT, currentLanguage.prompt)
                
                Log.d(TAG, "Intent extras - Language: $languageCode")
            }
            
            Log.d(TAG, "Starting recognition with locale: ${currentLanguage.locale}")
            speechRecognizer?.startListening(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition: ${e.message}", e)
            isListening = false
        }
    }
    
    fun stopListening() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
    }
    
    fun destroy() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying: ${e.message}")
        }
    }
}
