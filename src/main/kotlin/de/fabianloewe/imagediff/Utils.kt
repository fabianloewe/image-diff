package de.fabianloewe.imagediff

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

operator fun DiffResult.plus(other: DiffResult): DiffResult {
    if (this.first != other.first || this.second != other.second) {
        throw IllegalArgumentException("Images do not match")
    }

    val diff = this.diff.toMutableMap()
    other.diff.forEach { (key, value) ->
        diff[key] = value
    }
    return DiffResult(this.first, other.second, diff)
}