package com.maoserr.scrobjner

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.maoserr.scrobjner.controller.CameraController
import com.maoserr.scrobjner.controller.OnnxController
import com.maoserr.scrobjner.ui.theme.ScrobjnerTheme
import com.maoserr.scrobjner.ui.views.CameraView
import com.maoserr.scrobjner.ui.views.MainView

class MainActivity : ComponentActivity() {
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private var bitmap: MutableState<Bitmap?> = mutableStateOf(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.actionBar?.hide()
        val comp = this
        setContent {
            ScrobjnerTheme {
                LaunchedEffect(Unit) {
                    OnnxController.init(comp)
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = {
                                Text("Scrobjner")
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { shouldShowCamera.value = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Camera")
                        }
                    }
                ) { innerPadding ->
                    if (shouldShowCamera.value) {
                        CameraView(
                            bitmap,
                            onClose = ::handleClose,
                        )
                    } else {
                        MainView(bitmap, innerPadding)
                    }
                }
            }
        }
        CameraController.init(this)
    }


    private fun handleClose() {
        shouldShowCamera.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraController.release()
        OnnxController.release()
    }
}
