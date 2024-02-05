package de.fabianloewe.imagediff

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory

class DiffCommand(
    private val comparators: Map<String, ImageComparator>,
    private val coroutineContext: CoroutineContext,
) : CliktCommand() {
    private val first by option("-f", "--first")
        .path()
        .required()
        .help("The first file or directory containing images to compare")

    private val second by option("-s", "--second")
        .path()
        .required()
        .help("The second file or directory containing images to compare")

    private val comparatorsNames: Set<String> by option("-c", "--comparator")
        .multiple(default = comparators.keys.toList())
        .unique()
        .help("The comparator to use")

    override fun run() {
        if (first.isDirectory() && second.isDirectory()) {
            val firstFiles = first.toFile().walk().filter { it.isFile }.toList()
            val secondFiles = second.toFile().walk().filter { it.isFile }.toList()
            val pairs = firstFiles.zip(secondFiles)

            runBlocking(coroutineContext) {
                pairs.pmap { (first, second) -> compare(first.inputStream(), second.inputStream()) }
            }
        } else {
            compare(first.inputStream(), second.inputStream())
        }
    }

    private fun compare(first: InputStream, second: InputStream): Diff {
        return comparatorsNames
            .mapNotNull { comparators[it] }
            .map { it.compare(first, second) }
            .reduce { acc, diff -> acc + diff }
    }
}