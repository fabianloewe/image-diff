package com.github.fabianloewe.imagediff

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import me.tongfei.progressbar.ProgressBar
import org.koin.core.component.KoinScopeComponent
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.io.path.*

val <T : KoinScopeComponent> T.logger get() = scope.logger

@OptIn(ExperimentalPathApi::class)
fun Path.gatherFiles() = when {
    isDirectory() -> walk().filter { it.isRegularFile() }
    isRegularFile() -> sequenceOf(this)
    else -> throw IllegalArgumentException("Path is not a file or directory")
}

suspend fun <A, B> Sequence<A>.pmap(f: suspend (A) -> B): Flow<B> = callbackFlow {
    toList().map {
        launch {
            send(f(it))
        }
    }.joinAll()
    close()
}.buffer()

fun <T> Flow<T>.withProgressBar(
    progressBar: ProgressBar
): Flow<T> {
    return onEach { progressBar.step() }
        .onCompletion { progressBar.close() }
}

fun nameResultFileName(image: Image, extension: String): String {
    val imageName = image.path.name.replace('.', '_')
    return "$imageName.$extension"
}


fun nameResultFileName(coverImage: Image, stegoImage: Image, extension: String): String {
    val coverName = coverImage.path.name
    val stegoName = stegoImage.path.name
    val resultFileName = "${coverName}__${stegoName}".replace('.', '_')
    return "$resultFileName.$extension"
}

fun ByteArray.outputStream(): ByteArrayOutputStream {
    return ByteArrayOutputStream().also { it.write(this) }
}

/*
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

private fun JsonElement?.truncate(): JsonElement? {
        return when (this) {
            is JsonPrimitive -> {
                if (this.isString && this.content.length > maxValueLen) {
                    JsonPrimitive(this.content.take(maxValueLen - 1).let { "$it…" })
                } else this
            }

            is JsonObject -> JsonObject(mapValues { (_, value) -> value.truncate()!! })
            is JsonArray -> JsonArray(map { it.truncate()!! })
            else -> this
        }
    }
 */