package com.horizon.keyboard.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class VoiceLanguage(val code: String, val displayName: String) {
    ENGLISH("en-US", "EN"),
    BANGLA("bn-BD", "BN")
}

class VoiceRecognitionManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
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
    }
    
    fun startListening() {
        recognizedText = ""
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }
        
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }
                
                override fun onBeginningOfSpeech() {
                    isListening = true
                }
                
                override fun onRmsChanged(rmsdB: Float) {}
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    isListening = false
                }
                
                override fun onError(error: Int) {
                    isListening = false
                }
                
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizedText = text
                    if (text.isNotEmpty()) {
                        onResult?.invoke(text)
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizedText = text
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.code)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.code)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            
        } catch (e: Exception) {
            isListening = false
        }
    }
    
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        } catch (e: Exception) {
            // Ignore
        }
    }
}
