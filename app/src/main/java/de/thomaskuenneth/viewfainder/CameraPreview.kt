package de.thomaskuenneth.viewfainder

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(previewView: PreviewView, onClick: () -> Unit) {
    AndroidView(modifier = Modifier
        .fillMaxSize()
        .clickable {
            onClick()
        }, factory = { previewView })
}
