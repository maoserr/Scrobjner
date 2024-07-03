package com.maoserr.scrobjner.ui.views

import android.graphics.*
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maoserr.scrobjner.controller.OnnxController
import com.maoserr.scrobjner.utils.TouchableFeedback
import com.maoserr.scrobjner.utils.picker
import com.maoserr.scrobjner.utils.pickerW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
    val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
    val canvas = Canvas(bmOverlay)
    val paint = Paint()
    paint.color = Color.BLACK
    paint.blendMode = BlendMode.DST_ATOP
    canvas.drawBitmap(bmp1, Matrix(), paint)
    canvas.drawBitmap(bmp2, Matrix(), paint)
    return bmOverlay
}

class MainViewModel(
    val bit: MutableState<Bitmap?>
) : ViewModel() {
    val running: MutableState<Boolean> = mutableStateOf(false)
    val runtxt: MutableState<String> = mutableStateOf("")
    val outbit: MutableState<Bitmap?> = mutableStateOf(null)
    val overlayed: MutableState<Bitmap?> = mutableStateOf(null)
    fun runModel(offset: Offset, size: IntSize, pt1: Offset, pt2: Offset, bitchg: Boolean) {
        if ((!running.value) && (bit.value != null)) {
            running.value = true

            val bitm = bit.value!!
            Log.d("test", offset.toString())
            Log.d("test", size.toString())
            val w = offset.x * (bitm.width.toFloat() / size.width)
            val h = offset.y * (bitm.height.toFloat() / size.height)

            val nobox = (pt1 == Offset.Zero) && (pt2 == Offset.Zero)
            val minx = if (nobox) 0f else min(pt1.x, pt2.x)
            val maxx = if (nobox) bitm.width.toFloat() else max(pt1.x, pt2.x)
            val miny = if (nobox) 0f else min(pt1.y, pt2.y)
            val maxy = if (nobox) bitm.height.toFloat() else max(pt1.y, pt2.y)
            Log.i("Mao", "($w, $h), ($minx, $miny), ($maxx, $maxy)")
            viewModelScope.launch(Dispatchers.Default) {
                val modres:Bitmap
                val runtime:Float
//                if (bitchg) {
                    val res = OnnxController.runModel(
                        bitm,
                        Pair(w, h),
                        Pair(minx, miny), Pair(maxx, maxy)
                    )
                    modres = res.first
                    runtime = res.second
//                } else {
//                    val res = OnnxController.rerunDecode(
//                        Pair(w, h),
//                        Pair(minx, miny), Pair(maxx, maxy)
//                    )
//                    modres = res.first
//                    runtime = res.second
//                }
                Log.i("Mao","${bitm.width}, ${bitm.height}, " +
                        "${modres.width}, ${modres.height}")
                viewModelScope.launch(Dispatchers.Main) {
                    outbit.value = modres
                    overlayed.value = overlay(bitm, modres)
                    runtxt.value = "Took $runtime seconds."
                    running.value = false
                }
                Log.i("Mao", "Ran model.")
            }
        }
    }
}

@Composable
fun MainView(
    bitmap: MutableState<Bitmap?>,
    bitChg: MutableState<Boolean>,
    innerPadding: PaddingValues
) {
    val mod = remember { MainViewModel(bitmap) }
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Select image from gallery, " +
                "or click the Magnifying glass to pick from camera. " +
                "Then click on points to run model. " +
                "You can also drag a box to constrain model.")
        picker(mod.bit, bitChg)
        Row {
            if (mod.running.value) {
                CircularProgressIndicator(
                    modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                Text(text = mod.runtxt.value)
            }
        }
        TouchableFeedback(bit = mod.bit, outbit = mod.outbit)
        { offset, size, minC, maxC ->
            mod.runModel(offset, size, minC, maxC, bitChg.value)
            bitChg.value = false
        }

        mod.overlayed.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "out"
            )
            pickerW(bit = it)
        }
    }

}

