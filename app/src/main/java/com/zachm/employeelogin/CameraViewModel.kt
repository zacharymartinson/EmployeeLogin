package com.zachm.employeelogin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import kotlin.math.abs

class CameraViewModel : ViewModel() {

    val permissionGranted: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val detector: MutableLiveData<FaceDetector> by lazy { MutableLiveData<FaceDetector>() }

    private val _bboxes = MutableStateFlow<List<Rect>?>(null)
    val bboxes: StateFlow<List<Rect>?> get() = _bboxes

    fun updateCameraFeed(
        processCamera: ListenableFuture<ProcessCameraProvider>,
        surfaceProvider: Preview.SurfaceProvider,
        lifeCycleOwner: LifecycleOwner,
        thread: Executor,
        screenSize: IntSize
    ) {

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
                detectFaces(it, screenSize)
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
    fun detectFaces(proxy: ImageProxy, screenSize: IntSize) {
        proxy.image?.let { //Null check
            viewModelScope.launch {
                withTimeout(20000) {
                    try {
                        val inputImage = InputImage.fromMediaImage(it, proxy.imageInfo.rotationDegrees)
                        detector.value!!.process(inputImage)
                            .addOnSuccessListener {
                                val boxes = mutableListOf<Rect>()
                                val source = proxy.toBitmap()

                                it.forEach { face ->
                                    val box = face.boundingBox
                                    val scaledBox = getScaledRect(screenSize, IntSize(proxy.width, proxy.height), box, proxy.imageInfo.rotationDegrees)
                                    val cropped = Bitmap.createBitmap(source, box.left.coerceIn(0,box.width()), box.top.coerceIn(0,box.height()), box.width(), box.height())
                                    cropped.recycle()
                                    boxes.add(scaledBox)
                                }
                                _bboxes.value = boxes
                                source.recycle()

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

    /**
     * Most Android Cameras Use 420 but still good to check
     */
    private fun isYUV420(proxy: ImageProxy): Boolean {
        return proxy.format == ImageFormat.YUV_420_888
    }

    private fun createBitmapFromRect(proxy: ImageProxy, box: Rect) : Bitmap {
        val yBuffer = proxy.planes[0].buffer
        val uBuffer = proxy.planes[1].buffer
        val vBuffer = proxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        uBuffer.get(nv21, ySize, uSize)
        vBuffer.get(nv21, ySize + uSize, vSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, proxy.width, proxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(box, 100, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    /**
     * Scales the bounding box to the screen size allowing for different camera sizes.
     */
    private fun getScaledRect(screenSize: IntSize, cameraSize: IntSize, rect: Rect, rotation: Int): Rect {
        val isPortrait = rotation == 90 || rotation == 270

        val widthRatio = if(isPortrait) screenSize.width.toFloat() / cameraSize.height.toFloat() else screenSize.width.toFloat() / cameraSize.width.toFloat()
        val heightRatio = if(isPortrait) screenSize.height.toFloat() / cameraSize.width.toFloat() else screenSize.height.toFloat() / cameraSize.height.toFloat()

        val cameraAspectRatio = cameraSize.width.toFloat() / cameraSize.height.toFloat()
        val screenAspectRatio = screenSize.width.toFloat() / screenSize.height.toFloat()
        val aspectRatioDiff = abs(cameraAspectRatio - screenAspectRatio)

        val box = Rect(
            (rect.left * widthRatio).toInt(),
            (rect.top * heightRatio).toInt(),
            (rect.right * widthRatio).toInt(),
            (rect.bottom * heightRatio).toInt()
        )

        val padding = 0.8f

        when {
            isPortrait -> {
                return Rect(
                    box.left - (box.width() * padding).toInt(),
                    box.top - ((aspectRatioDiff * box.height())/4).toInt(),
                    box.right + (box.width() * padding).toInt(),
                    box.bottom + ((aspectRatioDiff * box.height())/4).toInt()
                )
            }
            else -> {
                return Rect(
                    box.left - ((aspectRatioDiff * box.width())/4).toInt(),
                    box.top - (box.height() * padding).toInt(),
                    box.right + ((aspectRatioDiff * box.width())/4).toInt(),
                    box.bottom + (box.height() * padding).toInt()
                )
            }
        }
    }


    fun getDetectorOptions() : FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .enableTracking()
            .build()
    }
}