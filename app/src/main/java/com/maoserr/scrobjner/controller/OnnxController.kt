package com.maoserr.scrobjner.controller

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.time.TimeSource

private const val DIM_PIXEL_SIZE = 4;

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
            comp.resources.openRawResource(R.raw.samenc_enh)
                .readBytes(), sessionOptions
        )
        inputEncName = ortSesEnc.inputNames.iterator().next()

        ortSesDec = ortEnv.createSession(
            comp.resources.openRawResource(R.raw.samdec_enh)
                .readBytes(), sessionOptions
        )
        inputDecName = ortSesDec.inputNames.iterator().next()
    }

    private fun decode(
        embeds: OnnxTensor,
        ptCoords1: FloatArray,
        ptLbls1: FloatArray,
    ): Pair<Float, Array<Array<ByteArray>>> {
        val mask = mk.zeros<Float>(1, 1, 256, 256).toFloatArray()
        val hasMask = mk.zeros<Float>(1).toFloatArray()
        val origIm = mk[684f, 1024f].toFloatArray()


        val ptCoords = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(ptCoords1), longArrayOf(1, 4, 2))
        val ptLbls = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(ptLbls1), longArrayOf(1, 4))
        val maskInput = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(mask), longArrayOf(1, 1, 256, 256))
        val hasMaskInp = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(hasMask), longArrayOf(1))
        val origImSize = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(origIm), longArrayOf(2))
        ptCoords.use {
            ptLbls.use {
                maskInput.use {
                    hasMaskInp.use {
                        origImSize.use {
                            val decInput = mapOf(
                                "image_embeddings" to embeds,
                                "point_coords" to ptCoords,
                                "point_labels" to ptLbls,
                                "mask_input" to maskInput,
                                "has_mask_input" to hasMaskInp,
                                "orig_im_size" to origImSize,
                                "_ppp2_orig_im_size" to origImSize
                            )
                            val out = ortSesDec.run(decInput)
                            out.use {
                                @Suppress("UNCHECKED_CAST")
                                val iou = (out[0].value as Array<FloatArray>)[0][0]

                                @Suppress("UNCHECKED_CAST")
                                val outMask = out[2].value as Array<Array<ByteArray>>
                                return iou to outMask
                            }
                        }
                    }
                }
            }
        }
    }

    fun runModel(
        img: Bitmap,
        pt: Pair<Float, Float>,
        tl: Pair<Float, Float>,
        br: Pair<Float, Float>
    ): Bitmap {
        val timeSource = TimeSource.Monotonic
        val markStart = timeSource.markNow()

        val imgBuf = ByteBuffer.allocate(
            img.width * img.height * DIM_PIXEL_SIZE
        )
        img.copyPixelsToBuffer(imgBuf)
        imgBuf.rewind()
        val outbuf = ByteBuffer.allocate(
            1024 * 684 * DIM_PIXEL_SIZE
        )
        val bmp = Bitmap.createBitmap(1024, 684, Bitmap.Config.ARGB_8888)
        val ptCoords1 = floatArrayOf(
            pt.first, pt.second,
            tl.first, tl.second,
            br.first, br.second,
            0.0f, 0.0f
        )
        Log.i("test", ptCoords1.contentToString())
        val ptLbls1 = floatArrayOf(0.0f, 2.0f, 3.0f, -1.0f)

        ortEnv.use {
            val imgDat = OnnxTensor.createTensor(
                ortEnv, imgBuf,
                longArrayOf(
                    img.height.toLong(),
                    img.width.toLong(),
                    DIM_PIXEL_SIZE.toLong()
                ), OnnxJavaType.UINT8
            )
            imgDat.use {
                val markPre = timeSource.markNow()
                val encInput = mapOf(inputEncName to imgDat)
                val out = ortSesEnc.run(encInput)
                out.use {
                    val markEnc = timeSource.markNow()
                    val rawOutput = out?.get(0)
                    val (iou, mask) = decode(rawOutput as OnnxTensor, ptCoords1, ptLbls1)
                    val markDec = timeSource.markNow()
                    val mask2 = mask.flatten().toTypedArray()
                    outbuf.rewind()
                    for (e in mask2) {
                        outbuf.put(e)
                    }
                    outbuf.rewind()
                    bmp.copyPixelsFromBuffer(outbuf)
                    val markEnd = timeSource.markNow()
                    Log.d("Mao", "Pre: ${markPre - markStart}")
                    Log.d("Mao", "Enc: ${markEnc - markPre}")
                    Log.d("Mao", "Dec: ${markDec - markEnc}")
                    Log.d("Mao", "Post: ${markEnd - markDec}")
                    Log.d("Mao", "Total: ${markEnd - markStart}")
                    return bmp
                }
            }
        }
    }

    fun release() {
        ortEnv.close()
    }
}
