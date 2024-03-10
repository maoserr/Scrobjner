package com.maoserr.scrobjner.controller

import ai.onnxruntime.*
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray
import java.lang.Float.min
import java.nio.FloatBuffer

private const val DIM_PIXEL_SIZE = 3;
private const val IMAGE_SIZE_X = 1024;
private const val IMAGE_SIZE_Y = 684;

private data class encResult(val embeds:OnnxTensor, val origw:Int, val origh: Int)

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

    private fun encode(img: Bitmap, decoder:(OnnxTensor)->Array<Array<Array<FloatArray>>>): Array<Array<Array<FloatArray>>> {
        val scaleX = IMAGE_SIZE_X.toFloat() / img.width
        val scaleY = IMAGE_SIZE_Y.toFloat() / img.height
        val scale = min(scaleX, scaleY)
        val mat = Matrix()
        mat.postScale(scale, scale)
        val resized = Bitmap.createBitmap(img, 0, 0, img.width, img.height, mat, false)
        img.recycle()
        ortEnv.use {
            val imgDat = img2Tensor(resized)
            imgDat.use {
                val encInput = mapOf(inputEncName to imgDat)
                val out = ortSesEnc.run(encInput)
                out.use {
                    val rawOutput = out?.get(0)
                    val res = decoder(rawOutput as OnnxTensor)
                    return res
                }
            }
        }
    }

    private fun decode(embeds:OnnxTensor):Array<Array<Array<FloatArray>>> {
        val ptCoords1= mk[
            mk[327.1111f,426.66666f],
            mk[241.77777f,341.33334f],
            mk[398.22223f,497.77777f],
            mk[0.0f,0.0f]].toNDArray().toFloatArray()
        val ptLbls1 = mk[mk[0.0f,2.0f,3.0f,-1.0f]].toNDArray().toFloatArray()
        val mask = mk.zeros<Float>(1,1,256,256).toFloatArray()
        val hasMask = mk.zeros<Float>(1).toFloatArray()
        val origIm = mk[684f,1024f].toFloatArray()


        val ptCoords = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(ptCoords1), longArrayOf(1,4,2))
        val ptLbls = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(ptLbls1), longArrayOf(1,4))
        val maskInput = OnnxTensor.createTensor(ortEnv,FloatBuffer.wrap(mask), longArrayOf(1,1,256,256))
        val hasMaskInp = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(hasMask), longArrayOf(1))
        val origImSize = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(origIm), longArrayOf(2))
        val decInput = mapOf(
            "image_embeddings" to embeds,
            "point_coords" to ptCoords,
            "point_labels" to ptLbls,
            "mask_input" to maskInput,
            "has_mask_input" to hasMaskInp,
            "orig_im_size" to origImSize
            )
        val out = ortSesDec.run(decInput)
        out.use {
            @Suppress("UNCHECKED_CAST")
            val res = out.get(0).value as Array<Array<Array<FloatArray>>>
            return res
        }
    }

    private fun img2Tensor(img: Bitmap): OnnxTensor {
        val imgData = FloatBuffer.allocate(
                    DIM_PIXEL_SIZE * img.width * img.height
        )
        imgData.rewind()
        val stride = img.width * img.height
        val bmpData = IntArray(stride)
        img.getPixels(bmpData, 0, img.width, 0, 0, img.width, img.height)
        for (i in bmpData.indices) {
            imgData.put(i * 3 + 2, (bmpData[i] shr 16 and 0xFF).toFloat())
            imgData.put( i * 3 + 1, (bmpData[i] shr 8 and 0xFF).toFloat())
            imgData.put( i * 3, (bmpData[i] and 0xFF).toFloat())
        }

        imgData.rewind()
        return OnnxTensor.createTensor(
            ortEnv, imgData,
            longArrayOf(
                img.height.toLong(),
                img.width.toLong(),
                DIM_PIXEL_SIZE.toLong()
            )
        )
    }

    fun runModel(img: Bitmap) {
        val masks = encode(img, ::decode)
        print("a")
    }
    fun release() {
        ortEnv.close()
    }
}
