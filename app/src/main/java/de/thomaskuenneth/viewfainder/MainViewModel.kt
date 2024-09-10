package de.thomaskuenneth.viewfainder

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val prompt = """
  Please describe what is contained inside the area of the image
  that is surrounded by a red line. If possible, add web links with
  additional information
"""
private const val modelName = "gemini-1.5-pro"

sealed interface UiState {
    data object Initial : UiState
    data object Loading : UiState
    data class Success(val outputText: String) : UiState
    data class Error(val errorMessage: String) : UiState
}

class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _bitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val bitmap = _bitmap.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = modelName, apiKey = BuildConfig.apiKey
    )

    fun setBitmap(bitmap: Bitmap?) {
        val old = _bitmap.value
        _bitmap.update { bitmap }
        old?.recycle()
    }

    fun getCopyOfBitmap(): Bitmap? =
        with(bitmap.value) { this?.copy(Bitmap.Config.ARGB_8888, true) }

    fun askGemini() {
        _bitmap.value?.let { bitmap ->
            sendPrompt(bitmap = bitmap)
        }
    }

    fun askGemini(bitmap: Bitmap) {
        sendPrompt(bitmap = bitmap)
    }

    fun reset() {
        _uiState.update { UiState.Initial }
    }

    private fun sendPrompt(bitmap: Bitmap) {
        _uiState.update { UiState.Loading }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}