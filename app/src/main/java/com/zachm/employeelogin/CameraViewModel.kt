package com.zachm.employeelogin

import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executor

class CameraViewModel : ViewModel() {

    val permissionGranted: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val detector: MutableLiveData<FaceDetector> by lazy { MutableLiveData<FaceDetector>() }


    private val _bboxes = MutableStateFlow<List<Rect>?>(null)
    val bboxes: StateFlow<List<Rect>?> get() = _bboxes

    private val _imageSize = MutableStateFlow<IntSize?>(null)
    val imageSize: StateFlow<IntSize?> get() = _imageSize

    fun updateCameraFeed(processCamera: ListenableFuture<ProcessCameraProvider>, surfaceProvider: Preview.SurfaceProvider, lifeCycleOwner: LifecycleOwner, thread: Executor) {

        if(permissionGranted.value == false) return

        processCamera.addListener({
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            val imageFuture = processCamera.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }

            //Gets us optimized frame times for frame size using camerax
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy(Size(1000, 1000), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(thread) {
                _imageSize.value = IntSize(it.width, it.height)
                detectFaces(it)
            }

            try {
                imageFuture.unbindAll()
                imageFuture.bindToLifecycle(lifeCycleOwner, cameraSelector, imageAnalysis, preview)
            }
            catch (e: Exception) {
                Log.d("CameraViewModel", "Exception: $e")
            }

        },thread)


    }


    @OptIn(ExperimentalGetImage::class)
    fun detectFaces(proxy: ImageProxy) {
        proxy.image?.let { ///Null check
            viewModelScope.launch {
                withTimeout(5000) {
                    try {
                        val inputImage = InputImage.fromMediaImage(it, proxy.imageInfo.rotationDegrees)
                        detector.value!!.process(inputImage)
                            .addOnSuccessListener {
                                val boxes = mutableListOf<Rect>()
                                it.forEach { face ->
                                    boxes.add(face.boundingBox)
                                }
                                _bboxes.value = boxes
                                proxy.close()
                            }
                            .addOnFailureListener {
                                Log.d("CameraViewModel", "Failure: $it")
                                proxy.close()
                            }
                    }
                    catch (e: Exception) {
                        Log.d("CameraViewModel", "Exception: $e")
                    }
                }
            }
        }
    }


    fun getDetectorOptions() : FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .enableTracking()
            .build()
    }
}