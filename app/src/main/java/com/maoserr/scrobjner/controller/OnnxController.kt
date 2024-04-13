package com.maoserr.scrobjner.controller

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.operations.toFloatArray
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.min

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

    fun scaleImg(img: Bitmap, targetW: Int = 1024, targetH: Int = 1024): Bitmap{
        val scaleX = targetW.toFloat() / img.width
        val scaleY = targetH.toFloat() / img.height
        val scale = min(scaleX, scaleY)
        val mat = Matrix()
        mat.postScale(scale, scale)
        val resized = Bitmap.createBitmap(img, 0, 0, img.width, img.height, mat, false)
        img.recycle()
        return resized
    }

    private fun decode(embeds:OnnxTensor):Pair<Float, Array<Array<ByteArray>>>  {
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

    fun runModel(img: Bitmap): Bitmap {
        val imgBuf = ByteBuffer.allocate(
            img.width*img.height* DIM_PIXEL_SIZE)
        img.copyPixelsToBuffer(imgBuf)
        imgBuf.rewind()
        val outbuf = ByteBuffer.allocate(
            1024*684* DIM_PIXEL_SIZE
        )
        val bmp = Bitmap.createBitmap(1024, 684, Bitmap.Config.ARGB_8888)

        ortEnv.use {
            val imgDat = OnnxTensor.createTensor(
                ortEnv, imgBuf,
                longArrayOf(
                    img.height.toLong(),
                    img.width.toLong(),
                    DIM_PIXEL_SIZE.toLong()
                ),OnnxJavaType.UINT8
            )
            imgDat.use {
                val encInput = mapOf(inputEncName to imgDat)
                val out = ortSesEnc.run(encInput)
                out.use {
                    val rawOutput = out?.get(0)
                    val (iou, mask) = decode(rawOutput as OnnxTensor)
                    val mask2 = mask.flatten().toTypedArray()
                    outbuf.rewind()
                    for (e in mask2){
                        outbuf.put(e)
                    }
                    outbuf.rewind()
                    bmp.copyPixelsFromBuffer(outbuf)
                    return bmp
                }
            }
        }
    }
    fun release() {
        ortEnv.close()
    }
}
