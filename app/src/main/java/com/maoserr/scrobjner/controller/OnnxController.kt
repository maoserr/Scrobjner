package com.maoserr.scrobjner.controller

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.maoserr.scrobjner.R


object OnnxController {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()

    fun init(comp:ComponentActivity){
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        val ortSesDec = ortEnv.createSession(comp.resources.openRawResource(R.raw.samdec)
            .readBytes(), sessionOptions)
        val ortSesEnc = ortEnv.createSession(comp.resources.openRawResource(R.raw.samenc)
            .readBytes(), sessionOptions)


    }

    fun release(){
        ortEnv.close()
    }
}
