package com.github.fabianloewe.imagediff.comparators

import com.github.fabianloewe.imagediff.*
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.composite.DifferenceComposite
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.math.abs

/**
 * Compares the pixels of two images.
 *
 * Pixels are compared by their RGB values and the differences are returned as a [DiffResult].
 * If the images have different dimensions, the comparison will take the pixels of the bigger image as they are as differences.
 * If one of the images is in grayscale, it will first be converted to RGB.
 *
 * Arguments:
 * - `bitDepth`: The bit depth to use for the comparison. Default: 8
 * - `ignoreAlpha`: Whether to ignore the alpha channel. Default: false
 * - `ignoreColorSpace`: Whether to ignore the color space. Default: false
 * - `colorChannels`: The color channels to compare. Can be "r", "g", "b", or a combination of those. Default: "rgb"
 *
 * @see ImageComparator
 */
class PixelComparator : ImageComparator {
    private class Args private constructor(args: ImageComparatorArgsMap) {
        val bitDepth: Int by args
        val ignoreAlpha: Boolean by args
        val ignoreColorSpace: Boolean by args
        val colorChannels: Set<ColorChannel> by args
        val diffImageDir: String by args

        val bitMask = (1 shl bitDepth) - 1

        companion object {
            val DEFAULTS = mapOf(
                "bitDepth" to 8,
                "ignoreAlpha" to false,
                "ignoreColorSpace" to false,
                "colorChannels" to "rgb".toColorChannels(),
                "diffImageDir" to "output/diff"
            )

            operator fun invoke(args: ImageComparatorArgsMap): Args {
                val parsedArgs = args.mapValues { (key, value) ->
                    when (key) {
                        "bitDepth" -> value.toString().toInt()
                        "ignoreAlpha" -> value.toString().toBoolean()
                        "ignoreColorSpace" -> value.toString().toBoolean()
                        "colorChannels" -> value.toString().toColorChannels()
                        "diffImageDir" -> value.toString()
                        else -> throw IllegalArgumentException("Unknown argument: $key")
                    }
                }
                return Args(DEFAULTS + parsedArgs)
            }
        }
    }

    override fun compare(coverImage: Image, stegoImage: Image, argsMap: ImageComparatorArgsMap): DiffResult {
        val args = Args(argsMap)

        val coverImageLoader = ImmutableImageLoader().type(TYPE_INT_ARGB)
        val cover = coverImageLoader.fromPath(coverImage.path)
        val stego = ImmutableImage.loader().type(TYPE_INT_ARGB).fromPath(stegoImage.path)
        val coverResized = if (cover.width > stego.width || cover.height > stego.height) {
            cover.padTo(stego.width, stego.height)
        } else cover
        val stegoResized = if (stego.width > cover.width || stego.height > cover.height) {
            stego.padTo(cover.width, cover.height)
        } else stego

        /*
        val coverPixels = coverResized.pixels()
        val stegoPixels = stegoResized.pixels()
        val diffs = coverPixels.zip(stegoPixels).map { (coverPixel, stegoPixel) ->
            val diffPixel = args.subtractPixels(coverPixel, stegoPixel)
            val key = DiffKey("${coverPixel.x},${coverPixel.y}")
            key to DiffValue(
                cover = coverPixel.toJsonElement(),
                stego = stegoPixel.toJsonElement(),
                diff = diffPixel.toJsonElement()
            )
        }.toMap()
         */

        val diffImage = coverResized.composite(DifferenceComposite(1.0), stegoResized)
        val diffImageName = "${coverImage.path.name}_${stegoImage.path.name}_diff.${coverImage.path.extension}"
        val writer = if (coverImage.path.extension == "png") PngWriter() else JpegWriter()
        diffImage.output(writer, "${args.diffImageDir}/$diffImageName")
        return DiffResult(
            coverImage,
            stegoImage,
            mapOf(NAME to mapOf(DiffKey("diff") to DiffValue(null, null, JsonPrimitive(diffImageName))))
        )
    }


    private fun Args.subtractPixels(coverPixel: Pixel, stegoPixel: Pixel): Pixel {
        return coverPixel.let { pixel ->
            if (!ignoreColorSpace && ColorChannel.R in colorChannels) {
                val value = abs(pixel.red() - stegoPixel.red())
                pixel.red(value and bitMask)
            } else pixel
        }.let { pixel ->
            if (!ignoreColorSpace && ColorChannel.G in colorChannels) {
                val value = abs(pixel.green() - stegoPixel.green())
                pixel.green(value and bitMask)
            } else pixel
        }.let { pixel ->
            if (!ignoreColorSpace && ColorChannel.B in colorChannels) {
                val value = abs(pixel.blue() - stegoPixel.blue())
                pixel.blue(value and bitMask)
            } else pixel
        }.let { pixel ->
            if (ignoreAlpha) {
                pixel
            } else {
                val value = abs(pixel.alpha() - stegoPixel.alpha())
                pixel.alpha(value and bitMask)
            }
        }
    }

    companion object {
        const val NAME = "pixel"
    }
}

private fun Pixel.toJsonElement(): JsonElement {
    val elements = listOf(
        JsonPrimitive(red()),
        JsonPrimitive(green()),
        JsonPrimitive(blue()),
        JsonPrimitive(alpha())
    )
    return JsonArray(elements)
}
