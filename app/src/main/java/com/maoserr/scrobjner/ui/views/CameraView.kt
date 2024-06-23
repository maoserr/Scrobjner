package com.maoserr.scrobjner.ui.views

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.maoserr.scrobjner.controller.CameraController
import com.maoserr.scrobjner.controller.FrameProc

/**
 * Tag for logging
 */
private const val TAG = "Scrobjner-CamView"


@Composable
fun CameraView(
    bitmap: MutableState<Bitmap?>,
    onClose: (() -> Unit)? = null,
) {
    val run: MutableState<Boolean> = remember { mutableStateOf(false) }
    val proc = remember { FrameProc(run, bitmap, onClose) }
    CameraController.BuildCamView(
        CameraSelector.LENS_FACING_BACK,
        LocalContext.current, LocalLifecycleOwner.current,
        proc
    )

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        CameraController.AddPreviewView()
        Row(Modifier.padding(bottom = 20.dp)) {
            IconButton(
                onClick = {
                    if (!run.value) {
                        run.value = true
                    }
                    Log.i(TAG, "Capturing photo...")
                },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.PlayArrow,
                        contentDescription = "Take picture",
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(1.dp)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            )
            IconButton(
                onClick = {
                    onClose?.invoke()
                },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(1.dp)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            )
        }
    }
}
