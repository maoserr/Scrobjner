package com.maoserr.scrobjner.controller

import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.maoserr.scrobjner.R
import com.maoserr.scrobjner.controller.CameraController.init
import com.maoserr.scrobjner.controller.CameraController.previewView
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Tag for logging
 */
private const val TAG = "Scrobjner-Cam"

/**
 * Controls main camera functionality.
 * Call [init] in the main loop before loading camera view,
 * then call [buildCamView] to build inside your view,
 * and add [previewView] to where you want the preview to show up
 */
object CameraController {
    private var cameraExec = Executors.newSingleThreadExecutor()
    var canShowCamera by mutableStateOf(false)
        private set

    // Init props
    private var analyzerProc: Analyzer? = null

    // View props
    private var previewView: PreviewView? = null
    private var imageCapture: ImageCapture? = null

    /**
     * Requests camera permission using [comp] and saves any
     * [capture] or [analyzer] setting.
     */
    fun init(
        comp: ComponentActivity,
        analyzer: Analyzer? = null
    ) {
        requestCameraPermission(
            comp
        ) {
            canShowCamera = true
            analyzerProc = analyzer
        }

    }

    /**
     * Releases camera
     */
    fun release() {
        cameraExec.shutdown()
    }

    private fun getOutputDirectory(comp: ComponentActivity): File {
        val mediaDir = comp.externalMediaDirs.firstOrNull()?.let {
            File(it, comp.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        Log.i(TAG, mediaDir.toString())
        Log.i(TAG, comp.filesDir.toString())
        return if (mediaDir != null && mediaDir.exists()) mediaDir else comp.filesDir
    }

    /**
     * Include this as the preview box
     */
    @Composable
    fun AddPreviewView() {
        if (!canShowCamera) {
            Log.e(TAG, "Camera not initialized")
            return
        }
        if (previewView == null) {
            Log.e(TAG, "Camera views not built")
            return
        }
        AndroidView({ previewView!! }, modifier = Modifier.fillMaxSize())
    }

    /**
     * Include this before the Camera view
     */
    @Composable
    fun BuildCamView(
        lensFacing: Int,
        context: Context,
        lifecycleOwner: LifecycleOwner
    ) {
        if (!canShowCamera) {
            Log.e(TAG, "Camera not initialized")
            return
        }
        val preview = Preview.Builder().build()
        previewView = remember { PreviewView(context) }
        val useCases: List<UseCase> = listOfNotNull(
            preview,
            analyzerProc?.let { anly ->
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExec, anly)
                    }
            }
        )
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        LaunchedEffect(lensFacing) {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            preview.setSurfaceProvider(previewView!!.surfaceProvider)
        }
    }

    /**
     * Gets camera permission from [comp], run [onSuccess] after.
     */
    private fun requestCameraPermission(
        comp: ComponentActivity,
        onSuccess: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                comp,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Permission previously granted")
                onSuccess()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                comp,
                android.Manifest.permission.CAMERA
            ) -> Log.i(TAG, "Show camera permissions dialog")

            else -> {
                val requestPermissionLauncher = comp.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Log.i(TAG, "Permission granted")
                        onSuccess()
                    } else {
                        Log.i(TAG, "Permission denied")
                    }
                }
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Required to launch Camera view
     */
    private suspend fun Context.getCameraProvider():
            ProcessCameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }
}
