package com.horizon.keyboard.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import com.horizon.keyboard.data.model.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for voice recognition.
 */
data class VoiceUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val currentLanguage: VoiceLanguage = VoiceLanguage.ENGLISH,
    val error: String? = null
)

/**
 * ViewModel for voice recognition functionality.
 * Manages SpeechRecognizer lifecycle and language switching.
 */
class VoiceViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "VoiceViewModel"
    }
    
    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()
    
    private var speechRecognizer: SpeechRecognizer? = null
    var onResult: ((String) -> Unit)? = null
    
    /**
     * Toggle between English and Bangla.
     */
    fun toggleLanguage() {
        stopListening()
        
        val newLanguage = if (_uiState.value.currentLanguage == VoiceLanguage.ENGLISH) {
            VoiceLanguage.BANGLA
        } else {
            VoiceLanguage.ENGLISH
        }
        
        _uiState.value = _uiState.value.copy(currentLanguage = newLanguage)
        Log.d(TAG, "Language switched to: ${newLanguage.displayName}")
    }
    
    /**
     * Start voice recognition.
     */
    fun startListening(context: Context) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            recognizedText = "",
            error = null
        )
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.value = _uiState.value.copy(error = "Speech recognition not available")
            return
        }
        
        try {
            cleanupRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
            startRecognition(currentState.currentLanguage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                isListening = false,
                error = "Failed to start: ${e.message}"
            )
        }
    }
    
    /**
     * Stop voice recognition.
     */
    fun stopListening() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            _uiState.value = _uiState.value.copy(isListening = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        cleanupRecognizer()
    }
    
    // Private methods
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _uiState.value = _uiState.value.copy(isListening = true)
            }
            
            override fun onBeginningOfSpeech() {
                _uiState.value = _uiState.value.copy(isListening = true)
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _uiState.value = _uiState.value.copy(isListening = false)
            }
            
            override fun onError(error: Int) {
                val errorMsg = getErrorMessage(error)
                Log.e(TAG, "Recognition error: $errorMsg")
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    error = errorMsg
                )
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Log.d(TAG, "Result: '$text'")
                
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    recognizedText = text
                )
                
                if (text.isNotEmpty()) {
                    onResult?.invoke(text)
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _uiState.value = _uiState.value.copy(recognizedText = text)
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun startRecognition(language: VoiceLanguage) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.locale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, language.prompt)
        }
        
        Log.d(TAG, "Starting recognition with locale: ${language.locale}")
        speechRecognizer?.startListening(intent)
    }
    
    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
    
    private fun getErrorMessage(error: Int): String = when (error) {
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
    
    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
