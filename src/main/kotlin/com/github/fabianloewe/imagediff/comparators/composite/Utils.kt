package com.github.fabianloewe.imagediff.comparators.composite

import com.sksamuel.scrimage.ImmutableImage

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


fun ImmutableImage.expectedFileExtension(): String? {
    return this.metadata
        .tagsBy { it.name == "Expected File Name Extension" }
        .firstOrNull()
        ?.value
        ?.lowercase()
}