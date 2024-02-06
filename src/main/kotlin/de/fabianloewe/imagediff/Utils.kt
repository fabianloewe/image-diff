package de.fabianloewe.imagediff

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.outputStream

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

operator fun DiffResult.plus(other: DiffResult): DiffResult {
    if (this.cover != other.cover || this.stego != other.stego) {
        throw IllegalArgumentException("Images do not match")
    }

    val diff = this.diff.toMutableMap()
    other.diff.forEach { (key, value) ->
        diff[key] = value
    }
    return copy(diff = diff)
}

/**
 * Get the output stream to the output file corresponding to this [DiffResult].
 * @receiver The [DiffResult] to write to a JSON file
 * @param baseDir The base directory to write to
 * @return The output stream
 */
fun DiffResult.outputStream(baseDir: Path): OutputStream {
    val fileName = cover.path.name + '_' + stego.path.name + ".jpg"
    return (baseDir / fileName).outputStream()
}
