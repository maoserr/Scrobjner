package com.maoserr.scrobjner.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun picker(imageUri: MutableState<Uri?>){

    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri.value = uri
    }
    Button(onClick = {
        launcher.launch("image/*")
    }) {
        Text(text = "select image")
    }
}

@Composable
fun TouchableFeedback(bit: MutableState<Uri>, cb: (offset: Offset, size: IntSize) -> Unit) {
    // Some constants here
    val sizeAnimationDuration = 200
    val colorAnimationDuration = 200
    val boxSize = 100.dp
    val startColor = Color.Red.copy(alpha = .05f)
    val endColor = Color.Red.copy(alpha = .8f)
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
        }
    )
    val sizeAnimation by animateDpAsState(
        if (visible) boxSize else 0.dp,
        tween(
            durationMillis = sizeAnimationDuration,
            easing = LinearEasing
        )
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
                        cb(it, size)
                    }
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
                    .height(if (visible) sizeAnimation else 0.dp)
                    .width(if (visible) sizeAnimation else 0.dp),
            )
        }
    }
}

