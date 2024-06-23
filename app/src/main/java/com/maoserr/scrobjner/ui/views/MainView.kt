package com.maoserr.scrobjner.ui.views

import android.content.res.loader.ResourcesLoader
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maoserr.scrobjner.R
import com.maoserr.scrobjner.controller.OnnxController
import com.maoserr.scrobjner.utils.TouchableFeedback
import com.maoserr.scrobjner.utils.picker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
    val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
    val canvas: Canvas = Canvas(bmOverlay)
    canvas.drawBitmap(bmp1, Matrix(), null)
    canvas.drawBitmap(bmp2, Matrix(), null)
    return bmOverlay
}

class MainViewModel():ViewModel() {
    val bit: MutableState<Bitmap?> = mutableStateOf(null)
    val outbit: MutableState<Bitmap?> = mutableStateOf(null)
    fun runModel(offset: Offset, size: IntSize, minC: Offset, maxC: Offset){
        if (bit.value != null) {


            val bitm = bit.value!!
            Log.d("test", offset.toString())
            Log.d("test", size.toString())
            val w = offset.x * (bitm.width.toFloat() / size.width)
            val h = offset.y * (bitm.height.toFloat() / size.height)

            val minx = minC.x
            val maxx = maxC.x
            val miny = minC.y
            val maxy = maxC.y
            Log.i("Mao", "($w, $h), ($minx, $miny), ($maxx, $maxy)")
            viewModelScope.launch(Dispatchers.Default) {
                val modres = OnnxController.runModel(
                    bitm,
                    Pair(w, h),
                    Pair(minx, miny), Pair(maxx, maxy)
                )
                outbit.value = modres
            }
            Log.i("Mao", "Ran model.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    comp: ComponentActivity,
    showCam: MutableState<Boolean> = mutableStateOf(false)
) {
    val mod = remember {MainViewModel()}
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
            picker(image = mod.bit)
            TouchableFeedback(bit = mod.bit, outbit = mod.outbit)
            { offset, size, minC, maxC ->
                mod.runModel(offset, size, minC, maxC)
            }

            mod.outbit.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "out"
                )
            }
        }
    }

}

