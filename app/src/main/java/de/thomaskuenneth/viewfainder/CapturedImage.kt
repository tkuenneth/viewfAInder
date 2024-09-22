package de.thomaskuenneth.viewfainder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun AnimatedCapturedImage(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var enabled by remember { mutableStateOf(true) }
    val weight by animateFloatAsState(
        if (enabled) 1f else WEIGHT_IMAGE,
        label = "weight"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.weight(weight)) {
            CapturedImage(viewModel)
        }
        if (weight < 1F) {
            Spacer(
                modifier = Modifier
                    .weight(1F - weight)
            )
        }
    }
    LaunchedEffect(uiState) {
        enabled = false
    }
}

@Composable
fun CapturedImage(viewModel: MainViewModel) {
    val bitmap by viewModel.capturedImage.collectAsState()
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
