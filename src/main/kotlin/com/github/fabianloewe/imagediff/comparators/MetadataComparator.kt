package com.github.fabianloewe.imagediff.comparators

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.fabianloewe.imagediff.Image
import com.github.fabianloewe.imagediff.ImageComparator
import com.github.fabianloewe.imagediff.ImageComparatorArgs
import com.github.fabianloewe.imagediff.ImageComparisonData
import com.github.fabianloewe.imagediff.comparators.metadata.DiffKey
import com.github.fabianloewe.imagediff.comparators.metadata.DiffResult
import com.github.fabianloewe.imagediff.comparators.metadata.DiffValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream

/**
 * Compares the metadata of two images.
 */
class MetadataComparator(
    private val json: Json,
) : ImageComparator {
    inner class Args : ImageComparatorArgs(this) {
        private val ignoreNullsOption = option(
            "--ignore-nulls",
            help = "Whether to ignore null values in the stego image.",
        ).flag("--no-ignore-nulls", default = true)

        /**
         * Whether to ignore null values in the stego image.
         */
        val ignoreNulls: Boolean by ignoreNullsOption

        private val maxValueLenOption = option("-L", "--max-value-len")
            .int()
            .default(100)
            .help("The maximum length of the value in the diff output. 0 means no limit. (default: 100)")

        /**
         * The maximum length of the value in the diff output. 0 means no limit.
         */
        val maxValueLen: Int by maxValueLenOption

        override val all: List<GroupableOption>
            get() = listOf(ignoreNullsOption, maxValueLenOption)
    }

    /**
     * The options for the metadata comparator.
     */
    override val args by lazy { Args() }

    /**
     * Compares the metadata of two images.
     * @see ImageComparator.compare
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun compare(coverImage: Image, stegoImage: Image): Result<ImageComparisonData> = runCatching {
        val coverMetadata = extractMetadata(coverImage)
        val stegoMetadata = extractMetadata(stegoImage)

        val coverMetadataDiff = coverMetadata
            .filterMetadataBy(stegoMetadata)
            .mapValues { (key, value) ->
                val coverValue = JsonPrimitive(value.truncate())
                val stegoValue = JsonPrimitive(stegoMetadata[key]?.truncate())
                DiffValue(coverValue, stegoValue)
            }
        val stegoMetadataDiff = stegoMetadata
            .filter { (key, value) ->
                // Here we want to include null values in the cover image to show metadata that was added in the stego image
                coverMetadata[key] != value
            }
            .mapValues { (key, value) ->
                val coverValue = JsonPrimitive(coverMetadata[key]?.truncate())
                val stegoValue = JsonPrimitive(value.truncate())
                DiffValue(coverValue, stegoValue)
            }
        val metadataDiff = coverMetadataDiff + stegoMetadataDiff
        val diffResult = DiffResult(
            coverImage.path.toString(),
            stegoImage.path.toString(),
            metadataDiff,
        )

        val outputStream = ByteArrayOutputStream()
        json.encodeToStream(diffResult, outputStream)
        val comparisonData = ImageComparisonData(
            this,
            coverImage,
            stegoImage,
            outputStream,
            "json",
        )
        comparisonData
    }

    private fun extractMetadata(image: Image): Map<DiffKey, String> {
        return image.data.metadata.tags().associate { DiffKey(it.name) to it.value }
    }

    private fun Map<DiffKey, String>.filterMetadataBy(stegoMetadata: Map<DiffKey, String>): Map<DiffKey, String> {
        return this.filter { (key, coverValue) ->
            stegoMetadata[key].let { stegoValue ->
                if (args.ignoreNulls && stegoValue == null) {
                    false
                } else {
                    coverValue != stegoValue
                }
            }
        }
    }

    private fun String.truncate(): String {
        return if (args.maxValueLen > 0 && this.length > args.maxValueLen) {
            this.take(args.maxValueLen - 1) + "…"
        } else {
            this
        }
    }
}