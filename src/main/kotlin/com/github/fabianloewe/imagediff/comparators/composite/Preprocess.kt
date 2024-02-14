package com.github.fabianloewe.imagediff.comparators.composite

import com.github.fabianloewe.imagediff.Image

/**
 * Pads the image to the size of the other image.
 */
private fun Image.padTo(other: Image): Image {
    return copy(_data = data.padTo(other.data.width, other.data.height))
}

/**
 * Crops the image to the size of the other image.
 */
private fun Image.cropTo(other: Image): Image {
    return copy(_data = data.resizeTo(other.data.width, other.data.height))
}

/**
 * Applies the preprocessing operation to the images.
 * @param image The image to apply the preprocessing operation to
 * @param other The other image to apply the preprocessing operation to
 * @return A pair of the preprocessed images
 */
operator fun ImagePreprocessing.invoke(image: Image, other: Image): Pair<Image, Image> {
    return when (this) {
        ImagePreprocessing.NONE -> image to other
        ImagePreprocessing.PAD -> {
            if (image.data.width < other.data.width || image.data.height < other.data.height) {
                image.padTo(other) to other
            } else {
                image to other.padTo(image)
            }
        }

        ImagePreprocessing.CROP -> {
            if (image.data.width > other.data.width || image.data.height > other.data.height) {
                image.cropTo(other) to other
            } else {
                image to other.cropTo(image)
            }
        }
    }
}