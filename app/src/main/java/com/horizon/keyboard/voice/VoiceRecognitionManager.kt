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

enum class VoiceLanguage(val code: String, val displayName: String, val prompt: String) {
    ENGLISH("en-US", "EN", "Speak in English..."),
    BANGLA("bn-BD", "BN", "বাংলায় বলুন...")
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
        currentLanguage = if (currentLanguage == VoiceLanguage.ENGLISH) {
            VoiceLanguage.BANGLA
        } else {
            VoiceLanguage.ENGLISH
        }
        Log.d(TAG, "Language switched to: ${currentLanguage.displayName} (${currentLanguage.code})")
    }
    
    fun startListening() {
        recognizedText = ""
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        
        try {
            // Destroy previous instance
            speechRecognizer?.destroy()
            speechRecognizer = null
            
            // Create new instance
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech in ${currentLanguage.displayName}")
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
                    Log.d(TAG, "Result: $text (language: ${currentLanguage.displayName})")
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
            
            // Create intent with explicit language
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                // Language model
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                
                // Set the language explicitly
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.code)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.code)
                
                // Also set the language model to prefer the selected language
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguage.code)
                
                // Enable partial results
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                
                // Max results
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                
                // Prompt message
                putExtra(RecognizerIntent.EXTRA_PROMPT, currentLanguage.prompt)
            }
            
            Log.d(TAG, "Starting recognition with language: ${currentLanguage.code}")
            speechRecognizer?.startListening(intent)
            isListening = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition: ${e.message}")
            isListening = false
        }
    }
    
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
    }
    
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying: ${e.message}")
        }
    }
}
