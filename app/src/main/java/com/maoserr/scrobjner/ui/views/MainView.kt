package com.maoserr.scrobjner.ui.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maoserr.scrobjner.controller.OnnxController

fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
    val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
    val canvas: Canvas = Canvas(bmOverlay)
    canvas.drawBitmap(bmp1, Matrix(), null)
    canvas.drawBitmap(bmp2, Matrix(), null)
    return bmOverlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun Greeting(
    showCam: MutableState<Boolean> = mutableStateOf(false),
    showPhoto: MutableState<Boolean> = mutableStateOf(false),
    photoUri: MutableState<Uri> = mutableStateOf(Uri.EMPTY),
) {
    val bit = BitmapFactory.decodeFile("/sdcard/Pictures/truck.jpg")
    val scbit = OnnxController.scaleImg(bit)
    val outbit: MutableState<Bitmap?> = remember { mutableStateOf(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Small Top App Bar")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Bottom app bar",
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCam.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                bitmap = scbit.asImageBitmap(),"Click"
            )
            Button(onClick = {
                val modres = OnnxController.runModel(scbit,
                    Pair(3f,3f),Pair(0f,0f),Pair(100f,100f))
                outbit.value = overlay(scbit, modres)
                Log.i("Mao", "Ran model.")
            }) {
                Text("Check")
            }

            outbit.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "out"
                )
            }
        }
    }

}

