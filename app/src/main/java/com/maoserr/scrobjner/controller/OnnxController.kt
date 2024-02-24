package com.maoserr.scrobjner.controller

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import java.lang.Float.min
import java.nio.FloatBuffer
import java.util.*

private const val DIM_BATCH_SIZE = 1;
private const val DIM_PIXEL_SIZE = 3;
private const val IMAGE_SIZE_X = 1024;
private const val IMAGE_SIZE_Y = 684;
private const val target_size = 1024

internal data class Result(
    var detectedIndices: List<Int> = emptyList(),
    var detectedScore: MutableList<Float> = mutableListOf<Float>(),
    var processTimeMs: Long = 0
) {}

object OnnxController {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()

    private lateinit var ortSesEnc: OrtSession
    private lateinit var inputEncName: String
    private lateinit var ortSesDec: OrtSession
    private lateinit var inputDecName: String

    fun init(comp: ComponentActivity) {
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())

        ortSesEnc = ortEnv.createSession(
            comp.resources.openRawResource(R.raw.samenc)
                .readBytes(), sessionOptions
        )
        inputEncName = ortSesEnc.inputNames.iterator().next()

        ortSesDec = ortEnv.createSession(
            comp.resources.openRawResource(R.raw.samdec)
                .readBytes(), sessionOptions
        )
        inputDecName = ortSesDec.inputNames.iterator().next()
    }

    fun encode(img: Bitmap): OrtSession.Result? {
        val scaleX = IMAGE_SIZE_X.toFloat() / img.width
        val scaleY = IMAGE_SIZE_Y.toFloat() / img.height
        val scale = min(scaleX, scaleY)
        val mat = Matrix()
        mat.postScale(scale, scale)
        val resized = Bitmap.createBitmap(img, 0, 0, img.width, img.height, mat, false)
        img.recycle()
        val imgDat = img2Tensor(resized)
        val encInput = mapOf(inputEncName to imgDat)
        val out = ortSesEnc.run(encInput)
        return out
    }

    fun img2Tensor(img: Bitmap): OnnxTensor {
        val imgData = FloatBuffer.allocate(
            DIM_BATCH_SIZE
                    * DIM_PIXEL_SIZE
                    * img.width
                    * img.height
        )
        imgData.rewind()
        val stride = img.width * img.height
        val bmpData = IntArray(stride)
        img.getPixels(bmpData, 0, img.width, 0, 0, img.width, img.height)
        for (i in 0..img.width - 1) {
            for (j in 0..img.height - 1) {
                val idx = img.height * i + j
                val pixelValue = bmpData[idx]
                imgData.put(idx, (pixelValue shr 16 and 0xFF).toFloat())
                imgData.put(idx + stride, (pixelValue shr 8 and 0xFF).toFloat())
                imgData.put(idx + stride * 2, (pixelValue and 0xFF).toFloat())
            }
        }

        imgData.rewind()
        return OnnxTensor.createTensor(
            ortEnv, imgData,
            longArrayOf(
                DIM_BATCH_SIZE.toLong(),
                DIM_PIXEL_SIZE.toLong(),
                img.width.toLong(),
                img.height.toLong()
            )
        )
    }

    fun runModel(img: Bitmap) {
        val res = encode(img)
        val shape = longArrayOf(1, 3, 224, 224)
        val imgData = preProcess(img)
        val tensor = OnnxTensor.createTensor(ortEnv, imgData, shape)
        val startTime = SystemClock.uptimeMillis()

        var result = Result()
        tensor.use {
            val output = ortSesDec.run(Collections.singletonMap(inputDecName, tensor))
            output.use {
                result.processTimeMs = SystemClock.uptimeMillis() - startTime
                @Suppress("UNCHECKED_CAST")
                val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]

            }
        }
    }

    fun preProcess(bitmap: Bitmap): FloatBuffer {
        val imgData = FloatBuffer.allocate(
            DIM_BATCH_SIZE
                    * DIM_PIXEL_SIZE
                    * IMAGE_SIZE_X
                    * IMAGE_SIZE_Y
        )
        imgData.rewind()
        val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
        val bmpData = IntArray(stride)
        bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0..IMAGE_SIZE_X - 1) {
            for (j in 0..IMAGE_SIZE_Y - 1) {
                val idx = IMAGE_SIZE_Y * i + j
                val pixelValue = bmpData[idx]
                imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.485f) / 0.229f))
                imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.456f) / 0.224f))
                imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.406f) / 0.225f))
            }
        }

        imgData.rewind()
        return imgData
    }


    fun release() {
        ortEnv.close()
    }
}
