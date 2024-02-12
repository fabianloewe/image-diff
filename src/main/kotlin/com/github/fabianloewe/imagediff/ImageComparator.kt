package com.github.fabianloewe.imagediff

typealias ImageComparatorArgsMap = Map<String, Any>

/**
 * Interface for comparing two images.
 */
interface ImageComparator {
    /**
     * Compares two images and returns the differences.
     *
     * @param coverImage The cover image.
     * @param stegoImage The stego image.
     * @param argsMap Additional arguments for the comparison.
     * @return The differences between the two images.
     */
    fun compare(coverImage: Image, stegoImage: Image, argsMap: ImageComparatorArgsMap): DiffResult
}