package com.zachm.employeelogin.ui.screens


import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    val boxes by viewModel.bboxes.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize(0,0)) }

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
            modifier = Modifier.fillMaxSize()
        ) {
            boxes?.let {
                it.forEach { rect->

                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                        size = Size(rect.width().toFloat(), rect.height().toFloat()),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

    }
}

@Preview
@Composable
private fun preview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        IconButton(
            onClick = { },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = "",
                tint = Color.Green,
                modifier = Modifier.size(32.dp)
            )
        }
    }


}