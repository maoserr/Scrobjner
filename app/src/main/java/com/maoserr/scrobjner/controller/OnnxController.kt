package com.maoserr.scrobjner.controller

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import com.maoserr.scrobjner.R


object OnnxController {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSesDec: OrtSession
    private lateinit var ortSesEnc: OrtSession

    fun init(comp:ComponentActivity){
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        ortSesDec = ortEnv.createSession(comp.resources.openRawResource(R.raw.samdec)
            .readBytes(), sessionOptions)
        ortSesEnc = ortEnv.createSession(comp.resources.openRawResource(R.raw.samenc)
            .readBytes(), sessionOptions)


    }

    fun runModel(img: Bitmap){

    }

    fun release(){
        ortEnv.close()
    }
}
