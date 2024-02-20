package com.github.fabianloewe.imagediff

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinScopeComponent
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.*

val <T : KoinScopeComponent> T.logger get() = scope.logger

@OptIn(ExperimentalPathApi::class)
fun Path.gatherFiles() = when {
    isDirectory() -> walk().filter { it.isRegularFile() }.toList()
    isRegularFile() -> listOf(this)
    else -> throw IllegalArgumentException("Path is not a file or directory")
}

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): Flow<B> = coroutineScope {
    map { async { f(it) } }.asFlow().map { it.await() }
}

operator fun DiffResult.plus(other: DiffResult): DiffResult {
    if (this.cover != other.cover || this.stego != other.stego) {
        throw IllegalArgumentException("Images do not match")
    }

    val diff = this.diff.toMutableMap()
    other.diff.forEach { (key, value) ->
        diff[key] = value
    }
    return DiffResult(cover, stego, diff = diff)
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

fun String.toColorChannels(): Set<ColorChannel> {
    return this.map { ColorChannel.valueOf(it.uppercase()) }.toSet()
}