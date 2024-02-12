package com.github.fabianloewe.imagediff

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

@Serializable
@JvmInline
value class DiffKey(val value: String)

@Serializable
data class DiffValue(
    val cover: JsonElement?,
    val stego: JsonElement?,
    val diff: JsonElement? = null
)

typealias ComparatorName = String

typealias Diff = Map<ComparatorName, Map<DiffKey, DiffValue>>

@JvmInline
value class Image(val path: Path)

enum class ColorChannel {
    R, G, B;
}

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
