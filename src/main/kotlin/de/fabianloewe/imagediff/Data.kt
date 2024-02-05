package de.fabianloewe.imagediff

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

typealias Diff = Map<String, Pair<String, String>>

@Serializable
data class Image(val path: Path, @Transient val inputStream: InputStream = path.inputStream())

@Serializable
data class DiffResult(val first: Image, val second: Image, val diff: Diff)

@Serializable
data class DiffData(
    val version: String,
    val timestamp: Long,
    val results: List<DiffResult>
)