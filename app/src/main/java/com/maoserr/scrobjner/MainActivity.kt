package com.maoserr.scrobjner

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
import com.maoserr.scrobjner.ui.theme.ScrobjnerTheme
import com.maoserr.scrobjner.ui.views.CameraView
import com.maoserr.scrobjner.ui.views.Greeting

class MainActivity : ComponentActivity() {
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

    private var showPhoto: MutableState<Boolean> = mutableStateOf(false)
    private var photoUri: MutableState<Uri> = mutableStateOf(Uri.EMPTY)


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
                    Greeting( shouldShowCamera, showPhoto, photoUri)
                }
            }
        }
        CameraController.init(this)
    }

    private fun handleImageCapture(uri: Uri) {
        Log.i("Mao", "Image captured: $uri")
        shouldShowCamera.value = false
        photoUri.value = uri
        showPhoto.value = true
    }

    private fun handleClose(){
        shouldShowCamera.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraController.release()
    }
}


@Preview(showBackground = true)
@Composable
fun testPreview() {
    ScrobjnerTheme {
        Greeting()
    }
}
