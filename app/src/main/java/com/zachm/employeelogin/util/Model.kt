package com.zachm.employeelogin.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.zachm.employeelogin.CameraViewModel
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import kotlin.math.exp

class Model(viewModel: CameraViewModel, val modelFile: MappedByteBuffer) {
    private val model: Interpreter = Interpreter(modelFile)


    @RequiresApi(Build.VERSION_CODES.Q)
    fun run(bitmap: Bitmap, rotation: Int): Embedding {
        val scaled = scaleAndRotateBitmap(bitmap, rotation)
        val input = createFloatTensor(scaled)
        val output = Array(1) { FloatArray(192) }

        model.run(input, output)

        return Embedding(output[0])
    }

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

    private fun scaleAndRotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        val scaleWidth = 112f / bitmap.width
        val scaleHeight = 112f / bitmap.height

        matrix.postScale(scaleWidth,scaleHeight)
        matrix.postRotate(rotation.toFloat())

        //Scales the file automatically, that's why we use the original height and width
        val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()

        return newBitmap
    }

    private fun sigmoid(x: Float): Float {
        return 1 / (1 + exp(-x))
    }
}