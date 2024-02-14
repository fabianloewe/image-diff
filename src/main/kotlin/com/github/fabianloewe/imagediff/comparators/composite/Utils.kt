package com.github.fabianloewe.imagediff.comparators.composite

import com.sksamuel.scrimage.pixels.Pixel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

fun String.toCompositeType(): CompositeType = when (this) {
    "diff" -> CompositeType.DIFF
    "subtract" -> CompositeType.SUBTRACT
    "overlay" -> CompositeType.OVERLAY
    "exclusion" -> CompositeType.EXCLUSION
    "darken" -> CompositeType.DARKEN
    "lighten" -> CompositeType.LIGHTEN
    else -> throw IllegalArgumentException("Unknown composite type: $this")
}

fun String.toImagePreprocessing(): ImagePreprocessing = when (this) {
    "none" -> ImagePreprocessing.NONE
    "pad" -> ImagePreprocessing.PAD
    "crop" -> ImagePreprocessing.CROP
    else -> throw IllegalArgumentException("Unknown preprocess operation: $this")
}

fun String.toImageSequenceCompositingMode(): ImageSequenceCompositingMode = when (this) {
    "normal" -> ImageSequenceCompositingMode.NORMAL
    "reduce-stego" -> ImageSequenceCompositingMode.REDUCE_STEGO
    "reduce-cover" -> ImageSequenceCompositingMode.REDUCE_COVER
    else -> throw IllegalArgumentException("Unknown mode: $this")
}

fun Pixel.isNotBlack(): Boolean = toRGB().any { it > 0 }

fun Pixel.toJsonArray(): JsonArray = JsonArray(
    listOf(
        JsonPrimitive(x),
        JsonPrimitive(y),
        JsonPrimitive(argb)
    )
)