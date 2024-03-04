package com.maoserr.scrobjner.controller

import ai.onnxruntime.*
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import java.lang.Float.min
import java.nio.FloatBuffer
import java.util.*

private const val DIM_PIXEL_SIZE = 3;
private const val IMAGE_SIZE_X = 1024;
private const val IMAGE_SIZE_Y = 684;

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

    private fun encode(img: Bitmap): OnnxValue? {
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
                    @Suppress("UNCHECKED_CAST")
                    val rawOutput = out?.get(0)
                    return rawOutput
                }
            }
        }
    }

    private fun decode(embeds:OnnxTensor) {
        //decoder_inputs = {
        //            "image_embeddings": image_embedding,
        //            "point_coords": onnx_coord,
        //            "point_labels": onnx_label,
        //            "mask_input": onnx_mask_input,
        //            "has_mask_input": onnx_has_mask_input,
        //            "orig_im_size": np.array(self.input_size, dtype=np.float32),
        //        }

        ortEnv.use {
            val ptCbuf = FloatBuffer.allocate(1)
            val ptLbuf = FloatBuffer.allocate(1)
            val maskBuf = FloatBuffer.allocate(1)
            val hasMaskBuf = FloatBuffer.allocate(1)
            val origImsbuf = FloatBuffer.allocate(1)

            val ptCoords = OnnxTensor.createTensor(ortEnv, ptCbuf)
            val ptLbls = OnnxTensor.createTensor(ortEnv, ptLbuf)
            val maskInput = OnnxTensor.createTensor(ortEnv, maskBuf)
            val hasMaskInp = OnnxTensor.createTensor(ortEnv, hasMaskBuf)
            val origImSize = OnnxTensor.createTensor(ortEnv, origImsbuf)
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

            }
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
        val embeds = encode(img)
        val res = decode(embeds as OnnxTensor)
    }
    fun release() {
        ortEnv.close()
    }
}
