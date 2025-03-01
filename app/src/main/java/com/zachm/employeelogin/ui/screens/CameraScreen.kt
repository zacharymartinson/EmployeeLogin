package com.zachm.employeelogin.ui.screens


import android.graphics.Rect
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.zachm.employeelogin.CameraViewModel
import java.util.concurrent.Executor

@Composable
fun CameraScreen(
    processCamera: ListenableFuture<ProcessCameraProvider>, lifeCycleOwner: LifecycleOwner, thread: Executor) {

    val viewModel: CameraViewModel = viewModel()

    val boxes by viewModel.bboxes.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize(0,0)) }
    val cameraSize by viewModel.imageSize.collectAsState()

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
                viewModel.updateCameraFeed(processCamera, it.surfaceProvider, lifeCycleOwner, thread)
            }
        )

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            boxes?.let {
                it.forEach { rect->

                    val scaledRect = getScaledRect(screenSize, cameraSize ?: IntSize(0,0), rect)

                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(scaledRect.left.toFloat(), scaledRect.top.toFloat()),
                        size = Size(scaledRect.width().toFloat(), scaledRect.height().toFloat()),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

    }
}

private fun getScaledRect(screenSize: IntSize, cameraSize: IntSize, rect: Rect): Rect {
    val widthRatio = screenSize.width.toFloat() / cameraSize.width.toFloat()
    val heightRatio = screenSize.height.toFloat() / cameraSize.height.toFloat()

    return Rect(
        (rect.left * widthRatio).toInt(),
        (rect.top * heightRatio).toInt(),
        (rect.right * widthRatio).toInt(),
        (rect.bottom * heightRatio).toInt()
    )
}

@Preview
@Composable
private fun preview() {

}