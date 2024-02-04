package com.maoserr.scrobjner

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.maoserr.scrobjner.controller.CameraController
import com.maoserr.scrobjner.ui.theme.ScrobjnerTheme
import com.maoserr.scrobjner.ui.views.CameraView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

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
                        outputDirectory = outputDirectory,
                        executor = cameraExecutor,
                        onImageCaptured = ::handleImageCapture,
                        onClose = ::handleClose,
                        onError = { Log.e("kilo", "View error:", it) }
                    )
                } else {
                    Greeting( shouldShowCamera, showPhoto, photoUri)
                }
            }
        }
        CameraController.initCamera(this,false)
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
        CameraController.releaseCamera()
    }
}


@Preview(showBackground = true)
@Composable
fun testPreview() {
    ScrobjnerTheme {
        Greeting()
    }
}
