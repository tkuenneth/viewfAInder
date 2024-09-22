package de.thomaskuenneth.viewfainder

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb

@Composable
fun CapturedImageAndDrawArea(viewModel: MainViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        CapturedImage(viewModel)
        Info()
        DrawingArea { size, offsets ->
            viewModel.capturedImage.value?.let { bitmap ->
                val xRatio = bitmap.width.toFloat() / size.width.toFloat()
                val yRatio = bitmap.height.toFloat() / size.height.toFloat()
                val scaledOffsets = offsets.map { point ->
                    PointF(point.x * xRatio, point.y * yRatio)
                }
                val canvas = Canvas(bitmap)
                val path = android.graphics.Path()
                if (scaledOffsets.isNotEmpty()) {
                    path.moveTo(scaledOffsets[0].x, scaledOffsets[0].y)
                    for (i in 1 until scaledOffsets.size) {
                        path.lineTo(scaledOffsets[i].x, scaledOffsets[i].y)
                    }
                    path.close()
                }
                canvas.drawPath(path, Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = STROKE_WIDTH / 2F
                    color = DRAWING_COLOR.toArgb()
                })
                viewModel.askGemini(bitmap)
            }
        }
    }
}
