package com.github.fabianloewe.imagediff

/**
 * Interface for comparing two images.
 */
interface ImageComparator {
    /**
     * Compares two images and returns the differences.
     *
     * @param coverImage The cover image.
     * @param stegoImage The stego image.
     * @param ignoreNulls Whether to ignore null values in the comparison (e.g. something was removed in the stego image).
     * @return The differences between the two images.
     */
    fun compare(coverImage: Image, stegoImage: Image, ignoreNulls: Boolean): DiffResult
}