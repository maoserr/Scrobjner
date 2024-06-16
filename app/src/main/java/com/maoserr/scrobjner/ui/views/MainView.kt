package com.maoserr.scrobjner.ui.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maoserr.scrobjner.controller.OnnxController
import com.maoserr.scrobjner.utils.TouchableFeedback
import com.maoserr.scrobjner.utils.picker


fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
    val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
    val canvas: Canvas = Canvas(bmOverlay)
    canvas.drawBitmap(bmp1, Matrix(), null)
    canvas.drawBitmap(bmp2, Matrix(), null)
    return bmOverlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    showCam: MutableState<Boolean> = mutableStateOf(false)
) {
    val bit: MutableState<Bitmap?> = remember { mutableStateOf(null) }
    val outbit: MutableState<Bitmap?> = remember { mutableStateOf(null) }
    val res = LocalContext.current.contentResolver
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
            picker(image = bit)
            var sliderPosition by remember { mutableFloatStateOf(0f) }
            Column {
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..400f
                )
                Text(text = sliderPosition.toString())
            }
            Button(onClick = {
                outbit.value = bit.value?.let {
                    OnnxController.runModel(
                        it,
                        Pair(327.1111f, 426.66666f),
                        Pair(241.77777f, 341.33334f), Pair(398.22223f, 497.77777f)
                    )
                }
            }) {
                Text("Check")
            }
            TouchableFeedback(bit = bit, outbit = outbit) { offset, size ->
                if (bit.value != null) {


                    val bitm = bit.value!!
                    val wOffSet = sliderPosition
                    val hOffSet = sliderPosition
                    Log.d("test", offset.toString())
                    Log.d("test", size.toString())
                    val w = offset.x * (bitm.width.toFloat() / size.width)
                    val h = offset.y * (bitm.height.toFloat() / size.height)

                    val minx = if (w > wOffSet) w - wOffSet else 0f
                    val maxx = if (w < (bitm.width - wOffSet)) w + wOffSet else bitm.width.toFloat()
                    val miny = if (h > hOffSet) h - hOffSet else 0f
                    val maxy = if (h < (bitm.height - hOffSet)) h + hOffSet else bitm.height.toFloat()
                    Log.i("Mao", "($w, $h), ($minx, $miny), ($maxx, $maxy)")
                    val modres = OnnxController.runModel(
                        bitm,
                        Pair(w, h),
                        Pair(minx, miny), Pair(maxx, maxy)
                    )
                    outbit.value = modres
                    Log.i("Mao", "Ran model.")
                }
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

