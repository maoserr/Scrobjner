package com.maoserr.scrobjner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

fun scaleImg(img: Bitmap, targetW: Int = 1024, targetH: Int = 1024): Bitmap {
    val scaleX = targetW.toFloat() / img.width
    val scaleY = targetH.toFloat() / img.height
    val scale = kotlin.math.min(scaleX, scaleY)
    val mat = Matrix()
    mat.postScale(scale, scale)
    val resized = Bitmap.createBitmap(img, 0, 0, img.width, img.height, mat, false)
    img.recycle()
    return resized
}

fun resizeSrc(bit: Bitmap, maxSize: Int = 1024): Bitmap {
    if (bit.width > maxSize && bit.width > bit.height){
        val scale = maxSize.toDouble() / bit.width
        val w = bit.width * scale
        val h = bit.height * scale
        val res = Bitmap.createScaledBitmap(bit, w.toInt(), h.toInt(), true)
        return res
    } else if (bit.height > maxSize && bit.height >= bit.width){
        val scale = maxSize.toDouble() / bit.height
        val w = bit.width * scale
        val h = bit.height * scale
        val res = Bitmap.createScaledBitmap(bit, w.toInt(), h.toInt(), true)
        return res
    }
    return bit
}

@Composable
fun picker(image: MutableState<Bitmap?>) {
    val res = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bit = BitmapFactory.decodeStream(res.openInputStream(uri))
            val sizedBit = resizeSrc(bit)
            image.value = sizedBit
        }

    }
    Button(onClick = {
        launcher.launch("image/*")
    }) {
        Text(text = "select image")
    }
}

@Composable
fun TouchableFeedback(
    bit: MutableState<Bitmap?>,
    outbit: MutableState<Bitmap?>,
    cb: (offset: Offset, size: IntSize) -> Unit
) {
    // Some constants here
    val sizeAnimationDuration = 200
    val colorAnimationDuration = 200
    val boxSize = 100.dp
    val startColor = Color.Green.copy(alpha = .05f)
    val endColor = Color.Green.copy(alpha = .8f)
    // These states are changed to update the animation
    var touchedPoint by remember { mutableStateOf(Offset.Zero) }
    var visible by remember { mutableStateOf(false) }

    // circle color and size in according to the visible state
    val colorAnimation by animateColorAsState(
        if (visible) startColor else endColor,
        animationSpec = tween(
            durationMillis = colorAnimationDuration,
            easing = LinearEasing
        ),
        finishedListener = {
            visible = false
        }, label = "colorAnim"
    )
    val sizeAnimation by animateDpAsState(
        if (visible) boxSize else 0.dp,
        tween(
            durationMillis = sizeAnimationDuration,
            easing = LinearEasing
        ), label = "sizeAnim"
    )
    // Box for the whole screen
    Box {
        // The touch offset is px and we need to convert to Dp
        val density = LocalDensity.current
        val (xDp, yDp) = with(density) {
            (touchedPoint.x.toDp() - boxSize / 2) to (touchedPoint.y.toDp() - boxSize / 2)
        }
        AsyncImage(
            model = bit.value,
            contentDescription = null,
            modifier = Modifier
                .pointerInput(Unit) {
                    val size = this.size
                    detectTapGestures {
                        touchedPoint = it
                        visible = true
//                        cb(it, size)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = {
                            offset ->
                        Log.d("Drag", "Start: $offset")
                    }, onDragEnd = {
                        Log.d("Drag", "End")
                    }){
                            _, offset ->
                        Log.d("Drag", "Draggin: $offset")
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (outbit.value != null) {
                        drawImage(outbit.value!!.asImageBitmap())
                    }
                }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            contentScale = ContentScale.Fit,
        )
        // This box serves as container. It has a fixed size.
        Box(
            Modifier
                .offset(xDp, yDp)
                .size(boxSize),
        ) {
            // And this box is animating the background and the size
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(colorAnimation, CircleShape)
                    .height(if (visible) sizeAnimation else 5.dp)
                    .width(if (visible) sizeAnimation else 5.dp),
            )
        }
    }
}

