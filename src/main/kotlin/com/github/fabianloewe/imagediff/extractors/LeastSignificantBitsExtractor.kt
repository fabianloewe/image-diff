package com.github.fabianloewe.imagediff.extractors

import com.github.fabianloewe.imagediff.Image
import com.github.fabianloewe.imagediff.ImageComparatorArgsMap
import com.github.fabianloewe.imagediff.ImageExtractor
import com.github.fabianloewe.imagediff.ImageExtractorArgsMap
import com.github.fabianloewe.imagediff.extractors.lsb.ColorChannel
import com.github.fabianloewe.imagediff.extractors.lsb.asBitMask
import com.github.fabianloewe.imagediff.extractors.lsb.toColorChannels
import com.sksamuel.scrimage.pixels.Pixel

/**
 * Extracts the least significant bits of the color channels of an image.
 */
class LeastSignificantBitsExtractor : ImageExtractor {
    private class Args private constructor(args: ImageComparatorArgsMap) {
        val colorChannels: Set<ColorChannel> by args
        val bitsPerChannel: Int by args

        val alphaBitMask by lazy { ColorChannel.ALPHA.asBitMask(bitsPerChannel) }
        val redBitMask by lazy { ColorChannel.RED.asBitMask(bitsPerChannel) }
        val greenBitMask by lazy { ColorChannel.GREEN.asBitMask(bitsPerChannel) }
        val blueBitMask by lazy { ColorChannel.BLUE.asBitMask(bitsPerChannel) }

        val useAlpha by lazy { ColorChannel.ALPHA in colorChannels }
        val useRed by lazy { ColorChannel.RED in colorChannels }
        val useGreen by lazy { ColorChannel.GREEN in colorChannels }
        val useBlue by lazy { ColorChannel.BLUE in colorChannels }

        fun Sequence<Pixel>.extractLSBs(): Sequence<IntArray> = map { pixel ->
            val (a, r, g, b) = pixel.toARGB()
            listOfNotNull(
                if (useAlpha) a and alphaBitMask else null,
                if (useRed) r and redBitMask else null,
                if (useGreen) g and greenBitMask else null,
                if (useBlue) b and blueBitMask else null
            ).toIntArray()
        }

        fun Sequence<IntArray>.mergeLSBsToBytes(): ByteArray {
            val bytes = mutableListOf<Byte>()
            var currentByte = 0
            var remainingBits = 8
            forEach { lsbArray ->
                lsbArray.forEach { lsb ->
                    if (remainingBits >= bitsPerChannel) {
                        currentByte = currentByte or (lsb shl (remainingBits - bitsPerChannel))
                        remainingBits -= bitsPerChannel
                    } else {
                        bytes += currentByte.toByte()
                        remainingBits = 8
                        currentByte = lsb shl (remainingBits - bitsPerChannel)
                        remainingBits -= bitsPerChannel
                    }
                }
            }
            return bytes.toByteArray()
        }

        companion object {
            val DEFAULTS = mapOf(
                "colorChannels" to setOf(ColorChannel.RED, ColorChannel.GREEN, ColorChannel.BLUE),
                "bitsPerChannel" to 8,
            )

            operator fun invoke(args: ImageComparatorArgsMap): Args {
                val parsedArgs = args.mapValues { (key, value) ->
                    when (key) {
                        "colorChannels" -> value.toString().toColorChannels()
                        "bitsPerChannel" -> value.toString().toInt()
                        else -> throw IllegalArgumentException("Unknown argument: $key")
                    }
                }
                return Args(DEFAULTS + parsedArgs)
            }
        }
    }

    override fun extract(image: Image, argsMap: ImageExtractorArgsMap): ByteArray {
        val args = Args(argsMap)
        val pixels = image.data.iterator().asSequence()
        return with(args) {
            pixels
                .extractLSBs()
                .mergeLSBsToBytes()
        }
    }

    companion object {
        const val NAME = "lsb"
    }
}