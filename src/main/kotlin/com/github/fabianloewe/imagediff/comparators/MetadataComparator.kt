package com.github.fabianloewe.imagediff.comparators

import com.github.fabianloewe.imagediff.*
import com.sksamuel.scrimage.metadata.ImageMetadata

/**
 * Compares the metadata of two images.
 */
class MetadataComparator : ImageComparator {
    private class Args private constructor(args: ImageComparatorArgsMap) {
        /**
         * Whether to ignore null values in the stego image.
         */
        val ignoreNulls: Boolean by args

        companion object {
            val DEFAULTS = mapOf("ignoreNulls" to true)

            operator fun invoke(args: ImageComparatorArgsMap): Args {
                return Args(args + DEFAULTS)
            }
        }
    }

    /**
     * Compares the metadata of two images.
     * @see ImageComparator.compare
     */
    override fun compare(coverImage: Image, stegoImage: Image, argsMap: ImageComparatorArgsMap): DiffResult {
        val args = Args(argsMap)
        val coverMetadata = extractMetadata(coverImage)
        val stegoMetadata = extractMetadata(stegoImage)

        val coverMetadataDiff = coverMetadata
            .filter { (key, coverValue) ->
                stegoMetadata[key].let { stegoValue ->
                    if (args.ignoreNulls && stegoValue == null) {
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

    private fun extractMetadata(image: Image): Map<DiffKey, String> {
        val metadata = ImageMetadata.fromPath(image.path)
        return metadata.tags().associate { DiffKey(it.name) to it.value }
    }

    companion object {
        const val NAME = "metadata"
    }
}