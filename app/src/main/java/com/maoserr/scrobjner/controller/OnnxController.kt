package com.maoserr.scrobjner.controller

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.time.TimeSource

private const val DIM_PIXEL_SIZE = 4;


object OnnxController {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()

    private lateinit var ortSesEnc: OrtSession
    private lateinit var inputEncName: String
    private lateinit var ortSesDec: OrtSession
    private lateinit var inputDecName: String

    private lateinit var encoded: FloatBuffer
    private var imgW: Int = 0
    private var imgH: Int = 0

    private val timeSource = TimeSource.Monotonic

    suspend fun init(comp: ComponentActivity) {
        val dispatcher: CoroutineDispatcher = Dispatchers.Default
        withContext(dispatcher) {
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
    }

    private fun decode(
        embeds: OnnxTensor,
        ptCoords1: FloatArray,
        ptLbls1: FloatArray,
        imgW: Int,
        imgH: Int
    ): Pair<Float, Array<Array<ByteArray>>> {
        val mask = mk.zeros<Float>(1, 1, 256, 256).toFloatArray()
        val hasMask = mk.zeros<Float>(1).toFloatArray()
        val origIm = mk[imgH.toFloat(), imgW.toFloat()].toFloatArray()


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

    suspend fun rerunDecode(
        pt: Pair<Float, Float>,
        tl: Pair<Float, Float>,
        br: Pair<Float, Float>
    ): Pair<Bitmap, Float> {
        val markStart = timeSource.markNow()
        val ptLbls1 = floatArrayOf(1.0f, 2.0f, 3.0f, -1.0f)
        val ptCoords1 = floatArrayOf(
            pt.first, pt.second,
            tl.first, tl.second,
            br.first, br.second,
            0.0f, 0.0f
        )
        val outbuf = ByteBuffer.allocate(
            imgW * imgH * DIM_PIXEL_SIZE
        )
        val markDec:TimeSource.Monotonic.ValueTimeMark
        val bmp = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888)
        ortEnv.use {
            val tens = OnnxTensor.createTensor(
                ortEnv, encoded, longArrayOf(
                    1L,
                    256,
                    64L,
                    64L
                )
            )
            tens.use {
                val (iou, mask) = decode(tens, ptCoords1, ptLbls1, imgW, imgH)

                markDec = timeSource.markNow()
                val mask2 = mask.flatten().toTypedArray()
                outbuf.rewind()
                for (e in mask2) {
                    outbuf.put(e)
                }
                outbuf.rewind()

                bmp.copyPixelsFromBuffer(outbuf)
            }
        }
        val markEnd = timeSource.markNow()
        Log.d("Mao", "Dec: ${markDec - markStart}")
        Log.d("Mao", "Post: ${markEnd - markDec}")

        var runtime: Float = 0f
        runtime = (markEnd - markStart).inWholeMilliseconds.toFloat() / 1000
        return Pair(bmp, runtime)
    }

    suspend fun runModel(
        img: Bitmap,
        pt: Pair<Float, Float>,
        tl: Pair<Float, Float>,
        br: Pair<Float, Float>
    ): Pair<Bitmap, Float> {
        val dispatcher: CoroutineDispatcher = Dispatchers.Default
        return withContext(dispatcher) {
            var bit: Bitmap
            var runtime: Float = 0f
            imgW = img.width
            imgH = img.height

            val markStart = timeSource.markNow()

            val imgBuf = ByteBuffer.allocate(
                img.width * img.height * DIM_PIXEL_SIZE
            )
            img.copyPixelsToBuffer(imgBuf)
            imgBuf.rewind()
            val outbuf = ByteBuffer.allocate(
                img.width * img.height * DIM_PIXEL_SIZE
            )
            val bmp = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
            val ptCoords1 = floatArrayOf(
                pt.first, pt.second,
                tl.first, tl.second,
                br.first, br.second,
                0.0f, 0.0f
            )
            Log.i("test", ptCoords1.contentToString())
            val ptLbls1 = floatArrayOf(1.0f, 2.0f, 3.0f, -1.0f)
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
                        val embeds = out?.get(0) as OnnxTensor
                        encoded = embeds.floatBuffer
                        val (iou, mask) = decode(embeds, ptCoords1, ptLbls1, img.width, img.height)
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
                        runtime = (markEnd - markStart).inWholeMilliseconds.toFloat() / 1000
                        bit = bmp
                    }
                }
            }
            return@withContext Pair(bit, runtime)
        }
    }

    fun release() {
        ortEnv.close()
    }
}
