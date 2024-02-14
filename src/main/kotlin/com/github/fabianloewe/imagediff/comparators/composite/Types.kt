package com.github.fabianloewe.imagediff.comparators.composite

/**
 * The type of compositing to use.
 */
enum class CompositeType {
    DIFF, SUBTRACT, OVERLAY, EXCLUSION, DARKEN, LIGHTEN
}

/**
 * A preprocessing operation to apply to the images before compositing them.
 */
enum class ImagePreprocessing {
    NONE, PAD, CROP
}

/**
 * The mode for applying the compositing operation.
 */
enum class ImageSequenceCompositingMode {
    NORMAL, REDUCE_COVER, REDUCE_STEGO
}