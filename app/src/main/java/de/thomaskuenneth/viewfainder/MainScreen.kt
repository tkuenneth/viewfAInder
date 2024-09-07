package de.thomaskuenneth.viewfainder

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    uiState: UiState,
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    askGemini: () -> Unit,
    reset: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        if (hasCameraPermission) {
            CameraPreview(previewView = previewView,
                onClick = { if (uiState !is UiState.Success) askGemini() })
        }

        when (uiState) {
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(0xa0000000))
                        .safeContentPadding()
                ) {
                    MarkdownText(
                        markdown = uiState.outputText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyLarge.merge(Color.White)
                    )
                    Button(
                        onClick = { reset() },
                        modifier = Modifier
                            .padding(all = 32.dp)
                            .align(Alignment.End)
                    ) {
                        Text(text = stringResource(id = R.string.done))
                    }
                }
            }

            is UiState.Error -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xa0000000))
                )
            }

            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Initial -> {
                val bitmap by viewModel.bitmap.collectAsState()
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .safeContentPadding()
                            .size(200.dp)
                    )
                }
                DrawingArea { size, points1 ->
                    viewModel.getCopyOfBitmap()?.let {
                        val xRatio = it.width.toFloat() / size.width.toFloat()
                        val yRatio = it.height.toFloat() / size.height.toFloat()
                        val points = points1.map { point ->
                            PointF(point.x * xRatio, point.y * yRatio)
                        }
                        val canvas = Canvas(it)
                        val path = android.graphics.Path()
                        if (points.isNotEmpty()) {
                            path.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x, points[i].y)
                            }
                            path.close()
                        }
                        canvas.drawPath(path, Paint().apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 12f
                            color = android.graphics.Color.RED
                        })
                        viewModel.askGemini(it)
                    }
                }
            }
        }
    }
}
