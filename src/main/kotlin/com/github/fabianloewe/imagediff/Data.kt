package com.github.fabianloewe.imagediff

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension

@Serializable
@JvmInline
value class DiffKey(val value: String)

@Serializable
data class DiffValue(val cover: String?, val stego: String?)

typealias ComparatorName = String

typealias Diff = Map<ComparatorName, Map<DiffKey, DiffValue>>

data class Image(val path: Path, @Transient val inputStream: InputStream = path.inputStream())

typealias DiffResultId = String

@Serializable
data class DiffResult(val id: String, val coverPath: String, val stegoPath: String, val diff: Diff) {
    constructor(cover: Image, stego: Image, diff: Diff) : this(
        cover.path.nameWithoutExtension + "_" + stego.path.nameWithoutExtension,
        cover.path.toString(),
        stego.path.toString(),
        diff
    )

    @Transient
    val cover: Image = Image(Path.of(coverPath))

    @Transient
    val stego: Image = Image(Path.of(stegoPath))
}

@Serializable
data class DiffData(
    val version: String,
    val timestamp: Long,
    val statistics: Statistics,
    val results: List<DiffResult>,
)

@Serializable
data class Statistics(
    val rateOfChanges: Map<ComparatorName, Map<DiffKey, Float>>,
    val changesPerImage: Map<DiffResultId, Int>,
) {
    @Transient
    val totalChanges: Int = changesPerImage.values.sum()

    @Transient
    val totalImages: Int = changesPerImage.size
}
