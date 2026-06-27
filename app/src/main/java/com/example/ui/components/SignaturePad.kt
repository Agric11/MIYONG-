package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CorporateBlue
import com.example.ui.theme.DarkGray

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureSaved: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var drawPath by remember { mutableStateOf(Path()) }
    val paths = remember { mutableStateListOf<Path>() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gambarkan Tanda Tangan Anda",
            color = DarkGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Signature Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, CorporateBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { motionEvent ->
                        val x = motionEvent.x
                        val y = motionEvent.y

                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                drawPath = Path().apply { moveTo(x, y) }
                                paths.add(drawPath)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                drawPath.lineTo(x, y)
                                // Trigger recomposition
                                val lastIndex = paths.size - 1
                                if (lastIndex >= 0) {
                                    paths[lastIndex] = drawPath
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                // Done drawing current path
                            }
                        }
                        true
                    }
            ) {
                drawIntoCanvas { composeCanvas ->
                    val nativeCanvas = composeCanvas.nativeCanvas
                    val paint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 8f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }

                    // Draw white background
                    nativeCanvas.drawColor(Color.WHITE)

                    // Draw all completed and active paths
                    for (path in paths) {
                        nativeCanvas.drawPath(path, paint)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    drawPath = Path()
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Hapus", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        // Create Bitmap representation of signature
                        val bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.TRANSPARENT)

                        val paint = Paint().apply {
                            color = Color.BLACK
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                            isAntiAlias = true
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }

                        // Determine bounds of drawn paths to auto-crop/scale nicely if needed
                        // For simplicity, we scale coordinates to fit standard signature box
                        // Let's draw paths to our bitmap
                        // Since paths coordinates are based on compose canvas size, we can scale them
                        // Or just draw directly (we can translate / scale)
                        // A simple solution is drawing exactly what was drawn
                        // To scale, we can compute bounds of paths
                        val bounds = android.graphics.RectF()
                        val combinedPath = Path()
                        for (p in paths) {
                            combinedPath.addPath(p)
                        }
                        combinedPath.computeBounds(bounds, true)

                        // Draw on bitmap canvas
                        // We translate coordinates to start at 20f, 20f and scale to fit 500x300
                        val margin = 30f
                        val scaleX = (500f - margin * 2) / if (bounds.width() > 0) bounds.width() else 500f
                        val scaleY = (300f - margin * 2) / if (bounds.height() > 0) bounds.height() else 300f
                        val scale = Math.min(scaleX, scaleY).coerceAtMost(1.5f)

                        canvas.save()
                        // Center drawing
                        val dx = (500f - bounds.width() * scale) / 2f - bounds.left * scale
                        val dy = (300f - bounds.height() * scale) / 2f - bounds.top * scale
                        canvas.translate(dx, dy)
                        canvas.scale(scale, scale)

                        canvas.drawPath(combinedPath, paint)
                        canvas.restore()

                        onSignatureSaved(bitmap)
                    }
                },
                enabled = paths.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue),
                modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp)
            ) {
                Text("Simpan", fontSize = 14.sp, color = androidx.compose.ui.graphics.Color.White)
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Batal", fontSize = 14.sp)
            }
        }
    }
}
