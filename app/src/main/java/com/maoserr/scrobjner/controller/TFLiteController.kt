package com.maoserr.scrobjner.controller

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import kotlin.time.TimeSource

class MinMaxScalingOp : TensorOperator {

    override fun apply( input : TensorBuffer?): TensorBuffer {
        val values = input!!.floatArray
        // Compute min and max of the output
        val max = values.maxOrNull()!!
        val min = values.minOrNull()!!
        for ( i in values.indices ) {
            // Normalize the values and scale them by a factor of 255
            var p = ((( values[ i ] - min ) / ( max - min )) * 255).toInt()
            if ( p < 0 ) {
                p += 255
            }
            values[ i ] = p.toFloat()
        }
        // Convert the normalized values to the TensorBuffer and load the values in it.
        val output = TensorBufferFloat.createFixedSize( input.shape , DataType.FLOAT32 )
        output.loadArray( values )
        return output
    }

}

object TFLiteController {
    private val timeSource = TimeSource.Monotonic
    private lateinit var interpreter: InterpreterApi
    private val inputImgProc = ImageProcessor.Builder()
        .add(ResizeOp(1024, 1024, ResizeOp.ResizeMethod.BILINEAR))
        .add(ResizeWithCropOrPadOp(1024, 1024))
        .build()
    private val outputTensorProcessor = TensorProcessor.Builder()
        .add( MinMaxScalingOp() )
        .build()

    suspend fun init(comp: ComponentActivity) {
        withContext(Dispatchers.Default) {
            Log.i("TFL", "Initialize...")
            val useGpu = Tasks.await(TfLiteGpu.isGpuDelegateAvailable(comp.applicationContext))
            Log.i("TFL", "GPU: $useGpu")
            Tasks.await(
                TfLite.initialize(
                    comp.applicationContext,
                    TfLiteInitializationOptions.builder().setEnableGpuDelegateSupport(useGpu).build()
                )
            )
            Log.i("TFL", "TF Init.")
            val interpreterOptions = InterpreterApi.Options()
                .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            if (useGpu){
                interpreterOptions.addDelegateFactory(GpuDelegateFactory())
                Log.i("TFL", "GPU Supported")
            } else {
                interpreterOptions.numThreads = 4
            }
            interpreter = InterpreterApi.create(
                FileUtil.loadMappedFile(comp.applicationContext, "encoder.tflite"),
                interpreterOptions
            )
            Log.i("TFL", "Encoder loaded.")
        }
    }
    suspend fun runModel(
        img: Bitmap,
        pt: Pair<Float, Float>,
        tl: Pair<Float, Float>,
        br: Pair<Float, Float>
        ):Pair<Bitmap, Float> {
        return withContext(Dispatchers.Default) {
            val markStart = timeSource.markNow()
            val inputTens = inputImgProc.process(TensorImage.fromBitmap(img))
            var outputTensor = TensorBufferFloat.createFixedSize(
                intArrayOf(1, 256 , 64 , 64 ) , DataType.FLOAT32 )

            val inputFl = TensorImage.createFrom(inputTens, DataType.FLOAT32)
            val markPreproc = timeSource.markNow()
            Log.d("TFL", "TFL Preproc: ${markPreproc - markStart}")
            interpreter.run( inputFl.buffer, outputTensor.buffer )
            val markEnd = timeSource.markNow()

            Log.d("TFL", "TFL Inf: ${markEnd - markStart}")
            val runtime = (markEnd - markStart).inWholeMilliseconds.toFloat() / 1000
            return@withContext Pair(img, runtime)
        }
    }
    fun release() {

    }
}
