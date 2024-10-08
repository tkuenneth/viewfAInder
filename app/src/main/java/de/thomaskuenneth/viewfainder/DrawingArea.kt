package de.thomaskuenneth.viewfainder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

const val STROKE_WIDTH = 12F
val DRAWING_COLOR = Color.Red

@Composable
fun DrawingArea(drawComplete: (IntSize, List<Offset>) -> Unit) {
    val points = remember { mutableStateListOf<Offset>() }
    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val touch = event.changes.first()
                    points.add(touch.position)
                    if (!touch.pressed) {
                        if (points.size > 2) {
                            drawComplete(size, points.toList())
                        }
                        points.clear()
                    }
                }
            }
        }) {
        if (points.size > 2) {
            drawPath(
                path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1..points.lastIndex) {
                        lineTo(points[i].x, points[i].y)
                    }
                    close()
                }, color = DRAWING_COLOR, style = Stroke(width = STROKE_WIDTH)
            )
        } else {
            points.forEach { point ->
                drawCircle(
                    color = DRAWING_COLOR, center = point, radius = STROKE_WIDTH / 2F
                )
            }
        }
    }
}
