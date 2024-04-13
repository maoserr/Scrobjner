package com.maoserr.scrobjner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.maoserr.scrobjner.controller.CameraController
import com.maoserr.scrobjner.controller.OnnxController
import com.maoserr.scrobjner.controller.SamAnalyzer
import com.maoserr.scrobjner.ui.theme.ScrobjnerTheme
import com.maoserr.scrobjner.ui.views.CameraView
import com.maoserr.scrobjner.ui.views.Greeting
import java.io.File

class MainActivity : ComponentActivity() {
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

    private var showPhoto: MutableState<Boolean> = mutableStateOf(false)
    private var photoUri: MutableState<Uri> = mutableStateOf(Uri.EMPTY)
    private var outbit: MutableState<Bitmap?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.actionBar?.hide()
        setContent {
            ScrobjnerTheme {
                if (shouldShowCamera.value) {
                    CameraView(
                        onClose = ::handleClose,
                    )
                } else {
                    Greeting( shouldShowCamera, showPhoto, photoUri,
                    )
                }
            }
        }
        OnnxController.init(this)
        CameraController.init(this, ::handleImageCapture,
            SamAnalyzer())
    }

    private fun handleImageCapture(file: File) {
        shouldShowCamera.value = false
        val uri = Uri.fromFile(file)
        val bit = BitmapFactory.decodeFile(file.absolutePath)
        val outbit = OnnxController.runModel(bit)
        Log.i("Mao", "Image captured: $uri")
        this.handleClose()
        photoUri.value = uri
        showPhoto.value = true
    }

    private fun handleClose(){
        shouldShowCamera.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraController.release()
        OnnxController.release()
    }
}


@Preview(showBackground = true)
@Composable
fun TestPreview() {
    ScrobjnerTheme {
        Greeting()
    }
}
