package de.thomaskuenneth.viewfainder

import android.graphics.Bitmap
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
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
import okhttp3.internal.toImmutableList

private const val modelName = "gemini-1.5-pro"

private val prompt_01 = """
  Describe what is contained inside the thick red line inside the image.
  Give a short description, followed by a bullet point list with all important details.
  Add web links with additional information for each bullet point items when available.
  If there are details related to appointments, locations, addresses,
  mention these explicitly
""".trimIndent()

private val prompt_02 = """
    Does the following text contain information that looks like
    a business card? Please answer only with yes or no.
    Here is the text: %s
""".trimIndent()

private val prompt_03 = """
    Please create a data structure in VCARD format.
    Do not add any explanations. Instead, make sure that your answer only
    contains the VCARD data structure, nothing else.
    Use the information that follows after the colon: %s
""".trimIndent()

enum class Action {
    VCARD
}

sealed interface UiState {
    data object Previewing : UiState
    data object Selecting : UiState
    data object Loading : UiState
    data class Error(val errorMessage: String) : UiState
    data class Success(val description: String, val actions: List<Pair<Action, String>>) : UiState
}

class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Previewing)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _bitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    private val bitmap = _bitmap.asStateFlow()

    private val _capturedImage: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val capturedImage = _capturedImage.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = modelName, apiKey = BuildConfig.apiKey
    )

    fun startSelecting() {
        _capturedImage.update { with(bitmap.value) { this?.copy(Bitmap.Config.ARGB_8888, true) } }
        _uiState.update { UiState.Selecting }
    }

    fun setBitmap(bitmap: Bitmap?) {
        val old = _bitmap.value
        _bitmap.update { bitmap }
        old?.recycle()
    }

    fun askGemini(bitmap: Bitmap) {
        sendPrompt(bitmap = bitmap)
    }

    fun reset() {
        _uiState.update { UiState.Previewing }
    }

    private fun sendPrompt(bitmap: Bitmap) {
        _uiState.update { UiState.Loading }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val actions = mutableListOf<Pair<Action, String>>()
                // First step: send the bitmap and get the description
                val description = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt_01)
                }).text ?: ""
                // Second step: Does the description contain appointment info?
                with(generativeModel.generateContent(content {
                    text(String.format(prompt_02, description))
                })) {
                    if (text?.toLowerCase(Locale.current)?.contains("yes") == true) {
                        with(generativeModel.generateContent(content {
                            text(String.format(prompt_03, description))
                        }).text) {
                            val data = this?.replace("```vcard", "")?.replace("```", "") ?: ""
                            if (data.isNotEmpty()) {
                                actions.add(Pair(Action.VCARD, data))
                            }
                        }
                    }
                }
                // Final step: update ui
                _uiState.value = UiState.Success(
                    description = description, actions = actions.toImmutableList()
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}