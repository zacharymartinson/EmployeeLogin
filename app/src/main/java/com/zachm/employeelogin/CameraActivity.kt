package com.zachm.employeelogin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.zachm.employeelogin.ui.screens.CameraScreen

class CameraActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        enableEdgeToEdge()

        viewModel.detector.value = FaceDetection.getClient(viewModel.getDetectorOptions())

        val processCamera = ProcessCameraProvider.getInstance(this)
        val thread = ContextCompat.getMainExecutor(this)

        setContent {
            CameraScreen(processCamera, this, thread)
        }
    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    private fun requestPermissions() {
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.CAMERA
        ))
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val allGranted = it.all { entry -> entry.value }
        viewModel.permissionGranted.value = allGranted
        Log.d("Permissions", "All permissions granted: $allGranted")

        it.entries.forEach { entry ->
            val permission = entry.key
            val granted = entry.value

            if(granted) {
                Log.d("Permissions", "$permission granted.")
            }
            else {
                Log.d("Permissions", "$permission denied.")
            }
        }
    }
}