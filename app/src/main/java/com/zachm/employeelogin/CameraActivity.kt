package com.zachm.employeelogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.lifecycle.ProcessCameraProvider
import com.zachm.employeelogin.ui.screens.CameraScreen

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val processCamera = ProcessCameraProvider.getInstance(this)

        setContent {
            CameraScreen(processCamera)
        }
    }
}