package com.github.fabianloewe.imagediff

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

@Serializable
@JvmInline
value class DiffKey(private val value: String) : Comparable<DiffKey> {
    override fun compareTo(other: DiffKey): Int {
        return value.compareTo(other.value)
    }
}

@Serializable
data class DiffValue(
    val cover: JsonElement?,
    val stego: JsonElement?,
    val diffv: JsonElement? = null
)

typealias ComparatorName = String

typealias Diff = Map<ComparatorName, Map<DiffKey, DiffValue>>

data class Image(val path: Path, private val _data: ImmutableImage? = null) {
    val data: ImmutableImage by lazy {
        _data ?: ImmutableImage.loader().type(BufferedImage.TYPE_INT_ARGB).fromPath(path)
    }
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

typealias ExtractedData = Map<String, ByteArray>

