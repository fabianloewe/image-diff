package com.github.fabianloewe.imagediff.comparators

import com.github.fabianloewe.imagediff.*
import com.github.fabianloewe.imagediff.comparators.composite.*
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import kotlinx.serialization.json.JsonPrimitive
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

    private class Args private constructor(args: ImageComparatorArgsMap) {
        val imageDir: String by args
        val composite: CompositeType by args
        val preprocess: ImagePreprocessing by args
        val mode: ImageSequenceCompositingMode by args

        companion object {
            val DEFAULTS = mapOf(
                "imageDir" to "output/composites",
                "composite" to CompositeType.DIFF,
                "preprocess" to ImagePreprocessing.NONE,
                "mode" to ImageSequenceCompositingMode.NORMAL
            )

            operator fun invoke(args: ImageComparatorArgsMap): Args {
                val parsedArgs = args.mapValues { (key, value) ->
                    when (key) {
                        "imageDir" -> value.toString()
                        "composite" -> value.toString().toCompositeType()
                        "preprocess" -> value.toString().toImagePreprocessing()
                        "mode" -> value.toString().toImageSequenceCompositingMode()
                        else -> throw IllegalArgumentException("Unknown argument: $key")
                    }
                }
                return Args(DEFAULTS + parsedArgs)
            }
        }
    }

    override fun compare(coverImage: Image, stegoImage: Image, argsMap: ImageComparatorArgsMap): DiffResult {
        val args = Args(argsMap)

        val (prepCoverImage, prepStegoImage) = args.preprocess(coverImage, stegoImage)

        val diffImage = when (args.mode) {
            ImageSequenceCompositingMode.NORMAL -> args.composite(prepCoverImage, prepStegoImage).copy(
                path = Path(args.imageDir) / args.diffImageName(prepCoverImage, prepStegoImage)
            )

            ImageSequenceCompositingMode.REDUCE_COVER -> {
                val previousImage = previousImageRef.get() ?: prepStegoImage
                val newImage = args.composite(previousImage, prepCoverImage).copy(
                    path = Path(args.imageDir) / args.diffImageName(previousImage, prepCoverImage),
                )
                previousImageRef.set(newImage)
                newImage
            }

            ImageSequenceCompositingMode.REDUCE_STEGO -> {
                val previousImage = previousImageRef.get() ?: prepCoverImage
                val newImage = args.composite(previousImage, prepStegoImage).copy(
                    path = Path(args.imageDir) / args.diffImageName(previousImage, prepStegoImage),
                )
                previousImageRef.set(newImage)
                newImage
            }
        }

        val writer = if (coverImage.path.extension == "png") PngWriter() else JpegWriter()
        diffImage.data.output(writer, diffImage.path)

        val diffStartPixel = diffImage.data.pixels().firstOrNull(Pixel::isNotBlack)?.let(Pixel::toJsonArray)
        val diffEndPixel = diffImage.data.pixels().lastOrNull(Pixel::isNotBlack)?.let(Pixel::toJsonArray)
        val diffValues = mapOf(
            DiffKey("Composite Type") to DiffValue(null, null, JsonPrimitive(args.composite.name.lowercase())),
            DiffKey("Composite Path") to DiffValue(null, null, JsonPrimitive(diffImage.path.toString())),
            DiffKey("Composite Start Pixel") to DiffValue(null, null, diffStartPixel),
            DiffKey("Composite End Pixel") to DiffValue(null, null, diffEndPixel),
        )

        return DiffResult(
            coverImage,
            stegoImage,
            mapOf(NAME to diffValues)
        )
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

    companion object {
        const val NAME = "composite"
    }
}