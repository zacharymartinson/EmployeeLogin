package com.zachm.employeelogin.ui.screens

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

@Composable
fun CameraScreen(processCamera: ListenableFuture<ProcessCameraProvider>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                //TODO Create ViewModel updateCamera connecting Context from Activity
            }
        }
    )
}

@Preview
@Composable
private fun preview() {

}