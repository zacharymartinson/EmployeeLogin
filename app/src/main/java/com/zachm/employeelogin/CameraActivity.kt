package com.zachm.employeelogin

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.zachm.employeelogin.ui.screens.CameraScreen
import com.zachm.employeelogin.util.Model
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class CameraActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        loadModelFile("FaceMobileNet.tflite")

        viewModel.detector.value = FaceDetection.getClient(viewModel.getDetectorOptions())
        viewModel.resetEmployeeMap()

        val processCamera = ProcessCameraProvider.getInstance(this)
        val thread = ContextCompat.getMainExecutor(this)

        enableEdgeToEdge()

        setContent {
            CameraScreen(processCamera, this, thread)
        }
    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    /**
     * Returns a MappedByteBuffer of the model file.
     */
    fun loadModelFile(fileName: String) {
        val descriptor = this.assets.openFd(fileName)
        val input = FileInputStream(descriptor.fileDescriptor)
        val channel = input.channel
        val file = channel.map(FileChannel.MapMode.READ_ONLY,descriptor.startOffset,descriptor.declaredLength)
        viewModel.modelFile.value = Model(viewModel, file)
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