package com.github.fabianloewe.imagediff.comparators

import com.github.fabianloewe.imagediff.*
import com.sksamuel.scrimage.metadata.ImageMetadata
import java.io.InputStream

/**
 * Compares the metadata of two images.
 */
class MetadataComparator : ImageComparator {
    /**
     * Compares the metadata of two images.
     * @see ImageComparator.compare
     */
    override fun compare(coverImage: Image, stegoImage: Image, ignoreNulls: Boolean): DiffResult {
        val coverMetadata = extractMetadata(coverImage.inputStream)
        val stegoMetadata = extractMetadata(stegoImage.inputStream)

        val metadataDiff = coverMetadata
            .filter { (key, value) ->
                stegoMetadata[key].let {
                    if (ignoreNulls && it == null) {
                        false
                    } else {
                        value != it
                    }
                }
            }
            .mapValues { (key, value) -> DiffValue(value, stegoMetadata[key]) }
        return DiffResult(coverImage, stegoImage, mapOf(NAME to metadataDiff))
    }

    private fun extractMetadata(inputStream: InputStream): Map<DiffKey, String> {
        val metadata = ImageMetadata.fromStream(inputStream)
        return metadata.tags().associate { DiffKey(it.name) to it.value }
    }

    companion object {
        const val NAME = "metadata"
    }
}