package de.fabianloewe.imagediff

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*

class DiffCommand(
    private val comparators: Map<String, ImageComparator>,
    private val coroutineContext: CoroutineContext,
    private val json: Json
) : CliktCommand() {
    private val coverImagePath by option("-c", "--cover")
        .path()
        .required()
        .help("The first file or directory containing images to compare")

    private val stegoImagePath by option("-s", "--stego")
        .path()
        .required()
        .help("The second file or directory containing images to compare")

    private val output by option("-o", "--output")
        .path()
        .required()
        .help("The file or directory to write the diff(s) to")

    private val splitOutput by option("--split")
        .flag(default = false)
        .help("Whether to split the output in multiple JSON files")

    private val comparatorsNames: Set<String> by option("--comparator")
        .multiple(default = comparators.keys.toList())
        .unique()
        .help("The comparator to use")

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    override fun run() {
        if (coverImagePath.isDirectory() && stegoImagePath.isDirectory()) {
            val firstFiles = coverImagePath.walk().filter { it.isRegularFile() }.toList()
            val secondFiles = stegoImagePath.walk().filter { it.isRegularFile() }.toList()
            val pairs = firstFiles.zip(secondFiles)

            val diffResults = runBlocking(coroutineContext) {
                pairs.pmap { (first, second) ->
                    compare(
                        Image(first, first.inputStream()),
                        Image(second, second.inputStream())
                    )
                }
            }

            if (splitOutput) {
                val dir = output.createDirectories()
                for (diffRes in diffResults) {
                    json.encodeToStream(diffRes, diffRes.outputStream(dir))
                }
            } else {
                val diffData = DiffData("1.0", System.currentTimeMillis(), diffResults)
                json.encodeToStream(diffData, output.outputStream())
            }
        } else {
            compare(
                Image(coverImagePath, coverImagePath.inputStream()),
                Image(stegoImagePath, stegoImagePath.inputStream())
            )
        }
    }

    private fun compare(first: Image, second: Image): DiffResult {
        return comparatorsNames
            .mapNotNull { comparators[it] }
            .map { it.compare(first, second) }
            .reduce { acc, diff -> acc + diff }
    }
}