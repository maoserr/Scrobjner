package com.maoserr.scrobjner.ui.views

import android.R.attr.bitmap
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    val bit = remember { mutableStateOf(Uri.EMPTY) }
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
            picker(imageUri = bit)
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
                val bit = BitmapFactory.decodeStream(res.openInputStream(bit.value))
                val modres = OnnxController.runModel(
                    bit,
                    Pair(327.1111f,426.66666f),
                    Pair(241.77777f,341.33334f), Pair(398.22223f,497.77777f)
                )
                outbit.value = overlay(bit, modres)
            }) {
                Text("Check")
            }
            TouchableFeedback(bit = bit) { offset, size ->
                val bit = BitmapFactory.decodeStream(res.openInputStream(bit.value))
                val wOffSet = sliderPosition
                val hOffSet = sliderPosition
                Log.d("test", offset.toString())
                Log.d("test", size.toString())
                val w = offset.x * (bit.width.toFloat() / size.width)
                val h = offset.y * (bit.height.toFloat() / size.height)

                val minx = if (w > wOffSet) w - wOffSet else 0f
                val maxx = if (w < (bit.width - wOffSet)) w + wOffSet else bit.width.toFloat()
                val miny = if (h > hOffSet) h - hOffSet else 0f
                val maxy = if (h < (bit.height - hOffSet)) h + hOffSet else bit.height.toFloat()
                Log.i("Mao", "($w, $h), ($minx, $miny), ($maxx, $maxy)")
                val modres = OnnxController.runModel(
                    bit,
                    Pair(w, h),
                    Pair(minx, miny), Pair(maxx, maxy)
                )
                outbit.value = overlay(bit, modres)
                Log.i("Mao", "Ran model.")
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

