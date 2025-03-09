package com.zachm.employeelogin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
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
import com.zachm.employeelogin.util.Candidate
import com.zachm.employeelogin.util.Embedding
import com.zachm.employeelogin.util.Employee
import com.zachm.employeelogin.util.Model
import com.zachm.employeelogin.util.TrackedFaces
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
    val modelFile: MutableLiveData<Model> by lazy { MutableLiveData<Model>() }
    val employees: MutableLiveData<HashSet<Employee>> by lazy { MutableLiveData<HashSet<Employee>>(hashSetOf()) }
    val employeeMap: MutableLiveData<HashMap<Int, Employee>> by lazy { MutableLiveData<HashMap<Int, Employee>>(hashMapOf()) }

    private val _trackedFaces = MutableStateFlow<MutableList<TrackedFaces>?>(null)
    val trackedFaces: StateFlow<MutableList<TrackedFaces>?> get() = _trackedFaces

    private val _showAddScreen = MutableStateFlow<Boolean?>(false)
    val showAddScreen: StateFlow<Boolean?> get() = _showAddScreen

    private val _faceBitmap = MutableStateFlow<Bitmap?>(null)
    val faceBitmap: StateFlow<Bitmap?> get() = _faceBitmap

    @RequiresApi(Build.VERSION_CODES.Q)
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
                if(showAddScreen.value == false) detectFaces(it, screenSize)
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

    fun addNewEmployee(name: String, id: Int, embedding: Embedding, currentId: Int) {
        val currentTime = System.currentTimeMillis()
        val employee = Employee(name, id, mutableListOf(embedding), currentTime)
        if(!employees.value!!.contains(employee)) {
            employees.value!!.add(employee)
            employeeMap.value!![currentId] = employee
        }
    }


    /**
     * Runs both Face Detection and Face Recognition on the ImageProxy
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalGetImage::class)
    fun detectFaces(proxy: ImageProxy, screenSize: IntSize) {
        proxy.image?.let { //Null check
            viewModelScope.launch {
                withTimeout(20000) {
                    try {
                        Log.d("CameraViewModel", "Rotation: ${proxy.imageInfo.rotationDegrees}")
                        val inputImage = InputImage.fromMediaImage(it, proxy.imageInfo.rotationDegrees)
                        detector.value!!.process(inputImage)
                            .addOnSuccessListener {
                                val faces = mutableListOf<TrackedFaces>()

                                var source = proxy.toBitmap()

                                if(proxy.imageInfo.rotationDegrees != 0) {
                                    val matrix = Matrix()
                                    matrix.postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                                    source = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                                }

                                it.forEach { face ->
                                    val box = face.boundingBox
                                    var employee: Employee? = null
                                    val currentTime = System.currentTimeMillis()

                                    //Screen Stuff (UI)
                                    val scaledBox = getScaledRect(screenSize, IntSize(proxy.width, proxy.height), box, proxy.imageInfo.rotationDegrees)

                                    //Model Stuff
                                    val x = box.left.coerceIn(0, source.width - 1)
                                    val y = box.top.coerceIn(0, source.height - 1)
                                    val width = box.width().coerceAtMost(source.width - x)
                                    val height = box.height().coerceAtMost(source.height - y)

                                    val cropped = Bitmap.createBitmap(source, x, y, width, height)
                                    _faceBitmap.value = cropped.copy(Bitmap.Config.ARGB_8888, true)
                                    val embedding = modelFile.value!!.run(cropped, proxy.imageInfo.rotationDegrees)

                                    face.trackingId?.let { id ->
                                        if(employeeMap.value!!.contains(id)) {
                                            employeeMap.value!![id]!!.embeddings.forEach { employeeEmbedding ->
                                                val distance = employeeEmbedding.compareCosineSimilarity(embedding)
                                                Log.d("CameraViewModel", "Distance: $distance, EmbeddingStored: ${employeeEmbedding.embeddings[0]}, EmbeddingNew: ${embedding.embeddings[0]}")

                                                employee = employeeMap.value!![id]

                                                if(distance >= 0.8) {
                                                    employee!!.lastTracked = currentTime
                                                }

                                                if(currentTime - employee!!.lastTracked > 2000L) {
                                                    Log.d("CameraViewModel", "Removing Employee: $id")
                                                    employeeMap.value!!.remove(id)
                                                }
                                            }
                                        }
                                        else {
                                            var bestCandidate: Candidate? = null
                                            employees.value!!.forEach { employee ->
                                                employee.embeddings.forEach { employeeEmbedding ->
                                                    val distance = employeeEmbedding.compareCosineSimilarity(embedding)
                                                    Log.d("CameraViewModel", "Distance: $distance, EmbeddingStored: ${employeeEmbedding.embeddings[0]}, EmbeddingNew: ${embedding.embeddings[0]}, Candidate: $bestCandidate")

                                                    if(distance >= 0.8) {
                                                        if (bestCandidate == null || distance > bestCandidate!!.distance) {
                                                            bestCandidate = Candidate(distance, employee)
                                                        }
                                                    }
                                                }
                                            }
                                            bestCandidate?.let {
                                                employee = it.employee
                                                employeeMap.value!![id] = employee!!
                                            }
                                        }
                                    }

                                    //Unknown
                                    faces.add(TrackedFaces(face.trackingId ?: 0, scaledBox, employee, embedding))

                                }
                                _trackedFaces.value = faces
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

    /**
     * Converts the ImageProxy to a Bitmap using GPU YUV.
     */
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

        val box = Rect(
            (rect.left * widthRatio).toInt(),
            (rect.top * heightRatio).toInt(),
            (rect.right * widthRatio).toInt(),
            (rect.bottom * heightRatio).toInt()
        )

        val scaleX = -0.1f
        val scaleY = 0.15f

        when {
            isPortrait -> {
                return Rect(
                    box.left - (box.width() * scaleY).toInt(),
                    box.top - (box.height() * scaleX).toInt(),
                    box.right + (box.width() * scaleY).toInt(),
                    box.bottom + (box.height() * scaleX).toInt()
                )
            }
            else -> {
                return Rect(
                    box.left - (box.width() * scaleX).toInt(),
                    box.top - (box.height() * scaleY).toInt(),
                    box.right + (box.width() * scaleX).toInt(),
                    box.bottom + (box.height() * scaleY).toInt()
                )
            }
        }
    }

    fun resetEmployeeMap() { employeeMap.value = hashMapOf() }

    fun showAddScreen() { _showAddScreen.value = true }
    fun closeAddScreen() { _showAddScreen.value = false }


    fun getDetectorOptions() : FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .enableTracking()
            .build()
    }
}