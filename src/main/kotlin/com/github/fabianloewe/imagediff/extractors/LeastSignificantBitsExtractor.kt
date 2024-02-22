package com.github.fabianloewe.imagediff.extractors

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.fabianloewe.imagediff.*
import com.github.fabianloewe.imagediff.extractors.lsb.ColorChannel
import com.github.fabianloewe.imagediff.extractors.lsb.asBitMask
import com.github.fabianloewe.imagediff.extractors.lsb.toColorChannels
import com.sksamuel.scrimage.pixels.Pixel

/**
 * Extracts the least significant bits of the color channels of an image.
 */
class LeastSignificantBitsExtractor : ImageExtractor {
    inner class Args : ImageExtractorArgs(this) {
        private val colorChannelsOption = option(
            "--color-channels",
            help = "The color channels to extract the least significant bits from. Any combination of `a`, `r`, `g` and `b`. (Default: `rgb`)"
        ).convert { it.toColorChannels() }.default(setOf(ColorChannel.RED, ColorChannel.GREEN, ColorChannel.BLUE))
        private val colorChannels by colorChannelsOption

        private val bitsPerChannelOption = option(
            "--bits-per-channel",
            help = "The number of bits per color channel to extract. (Default: 8)"
        ).int().default(8)
        private val bitsPerChannel: Int by bitsPerChannelOption

        override val all: List<GroupableOption>
            get() = listOf(colorChannelsOption, bitsPerChannelOption)

        private val alphaBitMask by lazy { ColorChannel.ALPHA.asBitMask(bitsPerChannel) }
        private val redBitMask by lazy { ColorChannel.RED.asBitMask(bitsPerChannel) }
        private val greenBitMask by lazy { ColorChannel.GREEN.asBitMask(bitsPerChannel) }
        private val blueBitMask by lazy { ColorChannel.BLUE.asBitMask(bitsPerChannel) }

        private val useAlpha by lazy { ColorChannel.ALPHA in colorChannels }
        private val useRed by lazy { ColorChannel.RED in colorChannels }
        private val useGreen by lazy { ColorChannel.GREEN in colorChannels }
        private val useBlue by lazy { ColorChannel.BLUE in colorChannels }

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
    }

    override val name = "lsb"

    override val args by lazy { Args() }

    override fun extract(image: Image): Result<ImageExtractionData> {
        val pixels = image.data.iterator().asSequence()
        val data = with(args) {
            pixels
                .extractLSBs()
                .mergeLSBsToBytes()
                .outputStream()
        }

        val fileExtension = "bin"
        return Result.success(ImageExtractionData(this, image, data, fileExtension))
    }
}