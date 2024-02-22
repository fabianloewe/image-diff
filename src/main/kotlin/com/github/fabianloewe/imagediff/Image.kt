package com.github.fabianloewe.imagediff

import com.sksamuel.scrimage.ImmutableImage
import java.awt.image.BufferedImage
import java.nio.file.Path


data class Image(val path: Path, private val _data: ImmutableImage? = null) {
    val data: ImmutableImage by lazy {
        _data ?: ImmutableImage.loader().type(BufferedImage.TYPE_INT_ARGB).fromPath(path)
    }
}

