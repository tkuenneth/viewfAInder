package de.thomaskuenneth.viewfainder

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    uiState: UiState,
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    roleStatus: RoleStatus,
    shouldShowMessage: Boolean,
    requestRole: () -> Unit,
    hideMessage: () -> Unit,
    finish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val results: @Composable () -> Unit = {
        Results(
            viewModel = viewModel,
            uiState = uiState,
            roleStatus = roleStatus,
            shouldShowMessage = shouldShowMessage,
            requestRole = requestRole,
            hideMessage = hideMessage,
            scope = scope,
            finish = finish
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Previewing -> {
                CameraPreview(previewView = previewView,
                    hasCameraPermission = hasCameraPermission,
                    onClick = { viewModel.startSelecting() })
            }

            is UiState.Selecting -> {
                CapturedImageAndDrawArea(viewModel)
            }

            is UiState.Loading -> {
                AnimatedCapturedImage(viewModel)
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is UiState.Success -> {
                results()
            }

            is UiState.Error -> {
                results()
            }
        }
    }
}
