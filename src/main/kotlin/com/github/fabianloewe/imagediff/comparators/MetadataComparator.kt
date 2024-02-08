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

        val coverMetadataDiff = coverMetadata
            .filter { (key, coverValue) ->
                stegoMetadata[key].let { stegoValue ->
                    if (ignoreNulls && stegoValue == null) {
                        false
                    } else {
                        coverValue != stegoValue
                    }
                }
            }
            .mapValues { (key, value) -> DiffValue(value, stegoMetadata[key]) }
        val stegoMetadataDiff = stegoMetadata
            .filter { (key, value) ->
                // Here we want to include null values in the cover image to show metadata that was added in the stego image
                coverMetadata[key] != value
            }
            .mapValues { (key, value) -> DiffValue(coverMetadata[key], value) }
        val metadataDiff = coverMetadataDiff + stegoMetadataDiff
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