package com.zachm.employeelogin.ui.screens


import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.zachm.employeelogin.CameraViewModel
import com.zachm.employeelogin.R
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun CameraScreen(
    processCamera: ListenableFuture<ProcessCameraProvider>, lifeCycleOwner: LifecycleOwner, thread: Executor) {

    val viewModel: CameraViewModel = viewModel()

    val faces by viewModel.trackedFaces.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize(0,0)) }
    val textMeasurer = rememberTextMeasurer()
    val text = remember { textMeasurer.measure(text = "+", style = TextStyle(fontSize = 80.sp, color = Color.Green, fontWeight = FontWeight.Thin)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {

        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                viewModel.updateCameraFeed(processCamera, it.surfaceProvider, lifeCycleOwner, thread, screenSize)
            }
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { offset ->
                    faces?.let {
                        it.forEach { face ->
                            if(face.box.contains(offset.x.toInt(), offset.y.toInt())) {
                                viewModel.addNewEmployee("Zach", face.embedding, face.currentId)
                            }
                        }
                    }
                } }
        ) {
            faces?.let {

                it.forEach { face->

                    val color = if(face.employee != null) Color.Blue else Color.Green
                    val name = face.employee?.name ?: "Unknown"

                    drawRect(
                        color = color,
                        topLeft = Offset(face.box.left.toFloat(), face.box.top.toFloat()),
                        size = Size(face.box.width().toFloat(), face.box.height().toFloat()),
                        style = Stroke(width = 4f)
                    )

                    if(face.employee == null) {
                        drawText(
                            textLayoutResult = text,
                            topLeft = Offset(face.box.centerX().toFloat() - (text.size.width/2), face.box.centerY().toFloat() - (text.size.height/2))
                        )
                    }

                    drawText(
                        textLayoutResult = textMeasurer.measure(text = name, style = TextStyle(fontSize = 24.sp, color = color, fontWeight = FontWeight.Light)),
                        topLeft = Offset(face.box.left.toFloat(), face.box.top.toFloat() - 80f)
                    )
                }
            }

        }

    }
}

@Preview
@Composable
private fun preview() {



}