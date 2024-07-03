package com.maoserr.scrobjner.controller

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.MutableState
import com.maoserr.scrobjner.utils.resizeSrc

internal class FrameProc(
    private val run: MutableState<Boolean>,
    private val outimg: MutableState<Bitmap?>,
    private val bitchg: MutableState<Boolean>,
    private val onCap: (() -> Unit)?
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (run.value) {
            run.value = false
            image.image?.let {
                outimg.value = resizeSrc(image.toBitmap())
                bitchg.value = true
                onCap?.invoke()
            }
        }
        image.close()
    }
}
