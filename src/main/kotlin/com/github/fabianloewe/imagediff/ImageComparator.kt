package com.github.fabianloewe.imagediff

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import java.io.ByteArrayOutputStream

/**
 * Abstract class for the options of an image comparator.
 */
abstract class ImageComparatorArgs(
    imageComparator: ImageComparator,
) : OptionGroup(
    name = "${imageComparator.name.capitalize()} Comparator Options",
    help = "Options for the ${imageComparator.name} comparator."
), ImageProcessorArgs {
    abstract override val all: List<GroupableOption>
}

/**
 * The result of comparing two images.
 */
data class ImageComparisonData(
    val comparator: ImageComparator,
    val coverImage: Image,
    val stegoImage: Image,
    val diffOutput: ByteArrayOutputStream,
    val diffFileExtension: String,
)


/**
 * Interface for comparing two images.
 */
interface ImageComparator : ImageProcessor<ImageComparatorArgs> {
    /**
     * The name of the image comparator.
     */
    override val name: String
        get() = this::class.simpleName!!
            .removeSuffix("Comparator")
            .dashCase()

    /**
     * The options for the image comparator.
     */
    override val args: ImageComparatorArgs

    /**
     * Compares two images and returns the differences.
     *
     * @param coverImage The cover image.
     * @param stegoImage The stego image.
     * @return Either the path to a file containing the differences or an error message.
     */
    fun compare(coverImage: Image, stegoImage: Image): Result<ImageComparisonData>
}
