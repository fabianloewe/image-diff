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
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

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
        private val imageDirOption = option(
            "--image-dir",
            help = "The directory to save the composite images to."
        ).default("output/composites")

        val imageDir: String by imageDirOption

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

        override val all = listOf(imageDirOption, compositeOption, preprocessOption, modeOption)
    }

    /**
     * The options for the composite comparator.
     */
    override val args by lazy { Args() }

    override fun compare(coverImage: Image, stegoImage: Image): Result<ImageComparisonData> {
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
        return Result.success(comparisonData)
    }

    private fun Args.diffImageName(cover: Image, stego: Image): String {
        val coverName = cover.path.nameWithoutExtension
        val stegoName = stego.path.nameWithoutExtension
        val compositeName = composite.name.lowercase(Locale.getDefault())
        val extension = cover.path.extension
        if (extension != stego.path.extension) {
            throw IllegalArgumentException("Cover and stego images must have the same file extension")
        }

        return when (mode) {
            ImageSequenceCompositingMode.NORMAL -> "$coverName-$stegoName-$compositeName.$extension"
            ImageSequenceCompositingMode.REDUCE_COVER -> {
                if (compositeName in stegoName) "$stegoName-$compositeName.$extension"
                else "$stegoName.$extension"
            }

            ImageSequenceCompositingMode.REDUCE_STEGO -> {
                if (compositeName in coverName) "$coverName-$compositeName.$extension"
                else "$coverName.$extension"
            }
        }
    }

    private fun composeNormal(cover: Image, stego: Image): Image {
        return args.composite(cover, stego).copy(
            path = Path(args.imageDir) / args.diffImageName(cover, stego)
        )
    }

    private fun composeByReduceCover(cover: Image, stego: Image): Image {
        val previousImage = previousImageRef.get() ?: stego
        val newImage = args.composite(previousImage, cover).copy(
            path = Path(args.imageDir) / args.diffImageName(previousImage, cover),
        )
        previousImageRef.set(newImage)
        return newImage
    }

    private fun composeByReduceStego(stego: Image, cover: Image): Image {
        val previousImage = previousImageRef.get() ?: cover
        val newImage = args.composite(previousImage, stego).copy(
            path = Path(args.imageDir) / args.diffImageName(previousImage, stego),
        )
        previousImageRef.set(newImage)
        return newImage
    }

    companion object {
        const val NAME = "composite"
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
