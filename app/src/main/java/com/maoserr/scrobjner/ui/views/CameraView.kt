package com.maoserr.scrobjner.ui.views

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maoserr.scrobjner.controller.CameraController

/**
 * Tag for logging
 */
private const val TAG = "Scrobjner-CamView"


@Composable
fun CameraView(
    onClose: (()-> Unit)? = null,
) {
    CameraController.BuildCamView(CameraSelector.LENS_FACING_BACK,
        LocalContext.current, LocalLifecycleOwner.current)

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        CameraController.AddPreviewView()
        Row (Modifier.padding(bottom = 20.dp)){
            IconButton(
                onClick = {
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
