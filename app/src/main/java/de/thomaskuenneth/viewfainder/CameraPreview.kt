package de.thomaskuenneth.viewfainder

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    onClick: () -> Unit
) {
    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
            FloatingActionButton(
                onClick = onClick, modifier = Modifier.safeContentPadding()
            ) {
                Image(
                    painter = painterResource(R.drawable.baseline_camera_24),
                    contentDescription = stringResource(R.string.shutter)
                )
            }
        }
    }
}
