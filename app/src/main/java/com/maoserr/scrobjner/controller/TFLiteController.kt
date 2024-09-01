package com.maoserr.scrobjner.controller

import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tflite.java.TfLite
import com.maoserr.scrobjner.R
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import java.nio.ByteBuffer

object TFLiteController {
    private lateinit var interpreter: InterpreterApi

    fun init(comp: ComponentActivity) {
        val initializeTask: Task<Void> by lazy { TfLite.initialize(comp.applicationContext) }

        initializeTask.addOnSuccessListener {
            val useGpuTask = TfLiteGpu.isGpuDelegateAvailable(comp.applicationContext)
            val interpreterTask = useGpuTask.continueWith { task ->
                val interpreterOptions = InterpreterApi.Options()
                    .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                if (task.result) {
                    interpreterOptions.addDelegateFactory(GpuDelegateFactory())
                }
                interpreter = InterpreterApi.create(
                    ByteBuffer.wrap(comp.resources.openRawResource(R.raw.encoder).readBytes()),
                    interpreterOptions
                )
            }
        }.addOnFailureListener { e ->
            Log.e("Interpreter", "Cannot initialize interpreter", e)
        }
        lifecycleScope.launchWhenStarted { // uses coroutine
            initializeTask.await()
        }
        TfLite.initialize(context,
            TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(true)
                .build())
    }
}
