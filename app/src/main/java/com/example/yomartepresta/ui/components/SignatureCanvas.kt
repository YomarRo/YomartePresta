package com.example.yomartepresta.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    points: SnapshotStateList<Offset?>
) {
    Box(modifier = modifier.background(Color.White)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            points.add(offset)
                        },
                        onDrag = { change, _ ->
                            points.add(change.position)
                        },
                        onDragEnd = {
                            points.add(null)
                        }
                    )
                }
        ) {
            val path = Path()
            var isFirst = true

            points.forEach { point ->
                if (point == null) {
                    isFirst = true
                } else {
                    if (isFirst) {
                        path.moveTo(point.x, point.y)
                        isFirst = false
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }
            }

            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

fun createSignatureBitmap(points: List<Offset?>, width: Int, height: Int): ByteArray? {
    if (points.isEmpty() || width <= 0 || height <= 0) return null
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    val path = android.graphics.Path()
    var isFirst = true
    
    points.forEach { point ->
        if (point == null) {
            isFirst = true
        } else {
            if (isFirst) {
                path.moveTo(point.x, point.y)
                isFirst = false
            } else {
                path.lineTo(point.x, point.y)
            }
        }
    }
    
    canvas.drawPath(path, paint)
    
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
