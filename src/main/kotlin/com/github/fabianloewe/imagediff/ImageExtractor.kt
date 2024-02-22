package com.github.fabianloewe.imagediff

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import java.io.ByteArrayOutputStream

/**
 * The arguments for an image extractor.
 */
abstract class ImageExtractorArgs(
    imageExtractor: ImageExtractor,
) : OptionGroup(
    name = "${imageExtractor.name.capitalize()} Extractor Options",
    help = "Options for the ${imageExtractor.name} extractor."
), ImageProcessorArgs {
    abstract override val all: List<GroupableOption>
}

/**
 * The result of extracting data from an image.
 */
data class ImageExtractionData(
    val extractor: ImageExtractor,
    val image: Image,
    val extractOutput: ByteArrayOutputStream,
    val extractFileExtension: String,
)

/**
 * Interface for extracting data from an image.
 */
interface ImageExtractor {
    /**
     * The name of the image extractor.
     */
    val name: String
        get() = this::class.simpleName!!
            .removeSuffix("Extractor")
            .dashCase()

    /**
     * The options for the image extractor.
     */
    val args: ImageExtractorArgs

    /**
     * Extracts data from an image.
     *
     * @param image The image to extract data from.
     * @return The extracted data.
     */
    fun extract(image: Image): Result<ImageExtractionData>
}
