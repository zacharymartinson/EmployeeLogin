package com.zachm.employeelogin.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.face.Face
import com.zachm.employeelogin.CameraViewModel
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.ByteArrayOutputStream
import java.nio.MappedByteBuffer
import kotlin.math.exp

class Model(private val viewModel: CameraViewModel, modelFile: MappedByteBuffer) {

    private val model: Interpreter

    init {
        val delegateOptions = Interpreter.Options().apply {
            addDelegate(GpuDelegate())
            addDelegate(NnApiDelegate())
        }
        //TODO Figure out why delegates are slower.
        model = Interpreter(modelFile)
    }

    private fun rotateSource(source: Bitmap, rotation: Int): Bitmap {
        if(rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            val new = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            source.recycle() //Recycle old bitmap here
            return new
        }
        else {
            return source
        }
    }

    @SuppressLint("NewApi")
    fun run(faces: MutableList<Face>, proxy: ImageProxy, screenSize: IntSize) {
        val employeeMap = viewModel.employeeMap.value!!
        val employees = viewModel.employees.value!!

        //Have to convert to a bitmap (everytime we make a bitmap it is slow)
        var source = proxy.toBitmap()
        source = rotateSource(source, proxy.imageInfo.rotationDegrees)

        val trackedFaces = mutableListOf<TrackedFaces>()
        val threshold = 0.8f //Recommended by sirius who developed the weights
        val currentTime = System.currentTimeMillis()

        faces.forEach { face ->
            val box = face.boundingBox
            var employee: Employee? = null

            //Screen Stuff for Compose
            val scaledBox = getScaledRect(screenSize, IntSize(proxy.width, proxy.height), box, proxy.imageInfo.rotationDegrees)

            //Model Stuff
            val x = box.left.coerceIn(0, source.width - 1)
            val y = box.top.coerceIn(0, source.height - 1)
            val width = box.width().coerceAtMost(source.width - x)
            val height = box.height().coerceAtMost(source.height - y)

            val cropped = Bitmap.createBitmap(source, x, y, width, height)

            //For those who want to explore doing it with YUV (it didn't save me much.) Only works on 0 Rotation. Might add later.
            //val yuv = createBitmapFromRect(proxy, box)

            val embedding = inference(cropped, proxy.imageInfo.rotationDegrees)

            face.trackingId?.let { id ->
                if(employeeMap.contains(id)) {
                    employeeMap[id]!!.embeddings.forEach { employeeEmbedding ->
                        val distance = employeeEmbedding.compareCosineSimilarity(embedding)

                        employee = employeeMap[id]

                        if(distance >= threshold) {
                            employee!!.lastTracked = currentTime
                            employee!!.framesTracked += 2
                        }

                        //TODO We longin here by checking successful frames or time.
                        if(employee!!.framesTracked >= 10) {
                            viewModel.login()
                        }
                    }
                }
                else {
                    var bestCandidate: Candidate? = null

                    employees.forEach { employee ->
                        employee.value.embeddings.forEach { employeeEmbedding ->
                            val distance = employeeEmbedding.compareCosineSimilarity(embedding)

                            if(distance >= threshold) {
                                if (bestCandidate == null || distance > bestCandidate!!.distance) {
                                    bestCandidate = Candidate(distance, employee.value)
                                }
                            }
                        }
                    }
                    bestCandidate?.let {
                        employee = it.employee
                        employeeMap[id] = employee!!
                    }
                }
            }

            //Unknown case
            trackedFaces.add(TrackedFaces(face.trackingId ?: 0, scaledBox, employee, embedding))

            viewModel.employeeMap.value = employeeMap
            viewModel.employees.value = employees
            viewModel.updateFaceBitmap(cropped)
        }

        //Clear map of old times
        employeeMap.forEach { employee->
            if(currentTime - employee.value.lastTracked > 1000L) {
                employeeMap.remove(employee.key)
            }
            if(employee.value.framesTracked > 0) {
                employee.value.framesTracked--
            }
        }


        viewModel.updateTrackedFaces(trackedFaces)
        source.recycle()
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

        //-0.15 and 0.15 are the results from Aspect Ratio 1.0 - 0.85 (difference pos or neg depending if portrait)
        //I just have these here for getting them tighter. TODO I will make this a bit more dynamic next update. Look at history for the math.
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


    /**
     * Inferences the Face Recognition Model on a Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun inference(bitmap: Bitmap, rotation: Int): Embedding {
        val scaled = scaleAndRotateBitmap(bitmap, rotation)
        val input = createFloatTensor(scaled)
        val output = Array(1) { FloatArray(192) }

        model.run(input, output)

        return Embedding(output[0])
    }

    /**
     * Converts the Bitmap to a FloatArray (Tensor Like Input) in the size of 1,112,112,3
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createFloatTensor(bitmap: Bitmap) : Array<Array<Array<FloatArray>>> {
        val tensor = Array(1) { Array(112) { Array(112) { FloatArray(3) } } }

        for(x in 0 until 112) {
            for(y in 0 until 112) {
                val pixel = bitmap.getColor(x,y)

                tensor[0][x][y][0] = pixel.red()
                tensor[0][x][y][1] = pixel.green()
                tensor[0][x][y][2] = pixel.blue()
            }
        }

        return tensor
    }

    /**
     * Rotates and Scales the Bitmap to 112x112 using GPU Matrix Operations.
     */
    private fun scaleAndRotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        val scaleWidth = 112f / bitmap.width
        val scaleHeight = 112f / bitmap.height

        matrix.postScale(scaleWidth,scaleHeight)
        matrix.postRotate(rotation.toFloat())

        //Scales the file automatically, that's why we use the original height and width
        val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        //Edge Case for when floating point scales it to 111 or 113. We want to avoid this as it is iterative O(n^2)
        if(newBitmap.width != 112 || newBitmap.height != 112) return Bitmap.createScaledBitmap(newBitmap, 112, 112, true)

        return newBitmap
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
        vBuffer.get(nv21, ySize, uSize)
        uBuffer.get(nv21, ySize + uSize, vSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, proxy.width, proxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(box, 100, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }
}