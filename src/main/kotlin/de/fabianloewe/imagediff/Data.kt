package de.fabianloewe.imagediff

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

@Serializable
data class DiffValue(val cover: String?, val stego: String?)

typealias Diff = Map<String, DiffValue>

data class Image(val path: Path, @Transient val inputStream: InputStream = path.inputStream())

@Serializable
data class DiffResult(val coverPath: String, val stegoPath: String, val diff: Diff) {
    constructor(cover: Image, stego: Image, diff: Diff) : this(cover.path.toString(), stego.path.toString(), diff)

    @Transient
    val cover: Image = Image(Path.of(coverPath))

    @Transient
    val stego: Image = Image(Path.of(stegoPath))
}

@Serializable
data class DiffData(
    val version: String,
    val timestamp: Long,
    val results: List<DiffResult>
)