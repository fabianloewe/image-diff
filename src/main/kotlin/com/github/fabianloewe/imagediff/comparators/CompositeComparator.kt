package com.github.fabianloewe.imagediff.comparators

import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.fabianloewe.imagediff.Image
import com.github.fabianloewe.imagediff.ImageComparator
import com.github.fabianloewe.imagediff.ImageComparatorArgs
import com.github.fabianloewe.imagediff.ImageComparisonData
import com.github.fabianloewe.imagediff.comparators.composite.*
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * A comparator that compares two images by compositing them and returning the difference image.
 *
 * Arguments:
 * - `imageDir` (optional): The directory to save the composite images to. (Default: `output/composites`)
 * - `composite` (optional): The type of compositing to use. Can be one of `diff`, `subtract` or `overlay`. (Default: `diff`)
 * - `preprocess` (optional): A preprocessing operation to apply to the images before compositing them. Can be one of `none`, `pad` (pad to size of bigger image) or `crop` (crop to size of smaller image). (Default: `none`)
 * - `mode` (optional): The mode for applying the compositing operation. Can be one of `normal` (apply only on input images) or `reduce` (apply on input images and reduce the result). (Default: `normal`)
 *
 * @see ImageComparator
 */
class CompositeComparator : ImageComparator {
    private val previousImageRef: AtomicReference<Image?> = AtomicReference()

    inner class Args : ImageComparatorArgs(this) {
        private val compositeOption = option(
            "--composite",
            help = "The type of compositing to use."
        ).convert { it.toCompositeType() }.default(CompositeType.DIFF)

        val composite: CompositeType by compositeOption

        private val preprocessOption = option(
            "--preprocess",
            help = "A preprocessing operation to apply to the images before compositing them."
        ).convert { it.toImagePreprocessing() }.default(ImagePreprocessing.NONE)

        val preprocess: ImagePreprocessing by preprocessOption

        private val modeOption = option(
            "--mode",
            help = "The mode for applying the compositing operation."
        ).convert { it.toImageSequenceCompositingMode() }.default(ImageSequenceCompositingMode.NORMAL)

        val mode: ImageSequenceCompositingMode by modeOption

        override val all = listOf(compositeOption, preprocessOption, modeOption)
    }

    /**
     * The options for the composite comparator.
     */
    override val args by lazy { Args() }

    override fun compare(coverImage: Image, stegoImage: Image): Result<ImageComparisonData> = runCatching {
        val (prepCoverImage, prepStegoImage) = args.preprocess(coverImage, stegoImage)

        val diffImage = when (args.mode) {
            ImageSequenceCompositingMode.NORMAL -> composeNormal(prepCoverImage, prepStegoImage)
            ImageSequenceCompositingMode.REDUCE_COVER -> composeByReduceCover(prepCoverImage, prepStegoImage)
            ImageSequenceCompositingMode.REDUCE_STEGO -> composeByReduceStego(prepStegoImage, prepCoverImage)
        }

        val fileType = diffImage.data.metadata.tagsBy { it.name == "Format" }.firstOrNull()?.value
        val outputStream = diffImage.data.outputStream(fileType ?: "unknown")
        val comparisonData = ImageComparisonData(
            this,
            coverImage,
            stegoImage,
            outputStream,
            fileType ?: "img"
        )
        comparisonData
    }

    private fun composeNormal(cover: Image, stego: Image): Image {
        return args.composite(cover, stego)
    }

    private fun composeByReduceCover(cover: Image, stego: Image): Image {
        val previousImage = previousImageRef.get() ?: stego
        val newImage = args.composite(previousImage, cover)
        previousImageRef.set(newImage)
        return newImage
    }

    private fun composeByReduceStego(stego: Image, cover: Image): Image {
        val previousImage = previousImageRef.get() ?: cover
        val newImage = args.composite(previousImage, stego)
        previousImageRef.set(newImage)
        return newImage
    }
}

private fun ImmutableImage.outputStream(fileType: String): ByteArrayOutputStream {
    val outputStream = ByteArrayOutputStream()
    outputStream.use {
        when (fileType) {
            "png" -> PngWriter().write(this, metadata, it)
            "jpg" -> JpegWriter().write(this, metadata, it)
            else -> throw IllegalArgumentException("Unsupported image format: $fileType")
        }
    }
    return outputStream
}
