package com.maoserr.scrobjner.ui.views

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maoserr.scrobjner.controller.OnnxController


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun Greeting(
    showCam: MutableState<Boolean> = mutableStateOf(false),
    showPhoto: MutableState<Boolean> = mutableStateOf(false),
    photoUri: MutableState<Uri> = mutableStateOf(Uri.EMPTY)
) {
    var presses by remember { mutableIntStateOf(0) }
    val sdcard = Environment.getExternalStorageDirectory().absolutePath
    val bit = BitmapFactory.decodeFile("$sdcard/Pictures/truck.jpg")
    val img = bit.asImageBitmap()
    var outbit = bit
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
            Button(onClick = {
                val sdcard = Environment.getExternalStorageDirectory().absolutePath
                val bit = BitmapFactory.decodeFile("$sdcard/Pictures/truck.jpg")
                outbit = OnnxController.runModel(bit)
                Log.i("Mao", "Ran model.")
            }) {
                Text("Check")
            }
            Canvas(modifier = Modifier.fillMaxSize(),){
                drawImage(img)
                drawImage(outbit.asImageBitmap())
            }
        }
    }

}

