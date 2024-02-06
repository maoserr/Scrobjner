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
import androidx.compose.ui.tooling.preview.Preview as CompPrev

/**
 * Tag for logging
 */
private const val TAG = "Scrobjner-CamView"


@Preview
@Composable
fun CameraView(
    onClose: (()-> Unit)? = null,
) {
    // 1
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    CameraController.BuildCamView(lensFacing, context, lifecycleOwner)

    // 3
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        CameraController.AddPreviewView()
        Row (Modifier.padding(bottom = 20.dp)){
            IconButton(
                onClick = {
                    Log.i(TAG, "Capturing photo...")
                    CameraController.capturePhoto()
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
                    if (onClose != null) {
                        onClose()
                    }
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