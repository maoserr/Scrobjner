package com.maoserr.scrobjner.controller

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.MutableState

internal class FrameProc(
    private val run: MutableState<Boolean>,
    private val outimg: MutableState<Bitmap?>,
    private val onCap: (() -> Unit)?
): ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (run.value) {
            run.value = false
            image.image?.let {
                outimg.value = image.toBitmap()
                onCap?.invoke()
            }
        }
        image.close()
    }
}
