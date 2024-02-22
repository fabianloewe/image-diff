package com.github.fabianloewe.imagediff.extractors.lsb

fun String.toColorChannels(): Set<ColorChannel> {
    return this.map { ColorChannel.valueOf(it.uppercase()) }.toSet()
}

fun ColorChannel.asBitMask(bitsPerChannel: Int): Int {
    val mask = (1 shl bitsPerChannel) - 1
    return when (this) {
        ColorChannel.RED -> mask shl 16
        ColorChannel.GREEN -> mask shl 8
        ColorChannel.BLUE -> mask
        ColorChannel.ALPHA -> mask shl 24
    }
}

