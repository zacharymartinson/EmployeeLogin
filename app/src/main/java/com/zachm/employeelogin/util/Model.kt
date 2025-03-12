package com.zachm.employeelogin.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
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

        //Have to convert to a bitmap (everytime we make a bitmap it is slow)
        var source = proxy.toBitmap()
        source = rotateSource(source, proxy.imageInfo.rotationDegrees)

        val trackedFaces = mutableListOf<TrackedFaces>()
        val threshold = 0.8f //Recommended by sirius who developed the weights

        faces.forEach { face ->
            val box = face.boundingBox
            var employee: Employee? = null
            val currentTime = System.currentTimeMillis()

            //Screen Stuff for Compose
            val scaledBox = getScaledRect(screenSize, IntSize(proxy.width, proxy.height), box, proxy.imageInfo.rotationDegrees)

            //Model Stuff
            val x = box.left.coerceIn(0, source.width - 1)
            val y = box.top.coerceIn(0, source.height - 1)
            val width = box.width().coerceAtMost(source.width - x)
            val height = box.height().coerceAtMost(source.height - y)

            val cropped = Bitmap.createBitmap(source, x, y, width, height)

            val embedding = inference(cropped, proxy.imageInfo.rotationDegrees)
            val employeeMap = viewModel.employeeMap.value!!
            val employees = viewModel.employees.value!!

            face.trackingId?.let { id ->
                if(employeeMap.contains(id)) {
                    employeeMap[id]!!.embeddings.forEach { employeeEmbedding ->
                        val distance = employeeEmbedding.compareCosineSimilarity(embedding)
                        Log.d("Model", "Distance: $distance, EmbeddingStored: ${employeeEmbedding.embeddings[0]}, EmbeddingNew: ${embedding.embeddings[0]}")

                        employee = employeeMap[id]

                        if(distance >= threshold) {
                            employee!!.lastTracked = currentTime
                        }

                        // 1 seconds of smoothening for tracking
                        //TODO add login functionality here through viewModel (should probably have database stuff there too)
                        if(currentTime - employee!!.lastTracked > 1000L) {
                            Log.d("Model", "Removing Employee: $id")
                            employeeMap.remove(id)
                        }
                    }
                }
                else {
                    var bestCandidate: Candidate? = null
                    employees.forEach { employee ->
                        employee.embeddings.forEach { employeeEmbedding ->
                            val distance = employeeEmbedding.compareCosineSimilarity(embedding)
                            Log.d("Model", "Distance: $distance, EmbeddingStored: ${employeeEmbedding.embeddings[0]}, EmbeddingNew: ${embedding.embeddings[0]}, Candidate: $bestCandidate")

                            if(distance >= threshold) {
                                if (bestCandidate == null || distance > bestCandidate!!.distance) {
                                    bestCandidate = Candidate(distance, employee)
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
}