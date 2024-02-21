package com.github.fabianloewe.imagediff.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.fabianloewe.imagediff.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import me.tongfei.progressbar.ProgressBarBuilder
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.component.newScope
import org.koin.core.scope.Scope
import java.io.IOException
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*

class Diff(
    private val comparators: Map<String, ImageComparator>,

) : CliktCommand(), KoinScopeComponent {
    override val scope: Scope by newScope()

    private val progressBarBuilder: ProgressBarBuilder by inject()
    private val coroutineContext: CoroutineContext by inject()
    private val json: Json by inject()
    private val csvReader: CsvReader by inject()

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

    private val corListPath by option("-l", "--cor-list")
        .path()
        .help(
            """
            The file containing correspondences between the images as a CSV file.
             It must contain two columns: stego_image_filename and cover_image_filename. 
             If not provided, the images are compared in the order they are found in the directories.
            """.trimIndent()
        )

    private val splitOutput by option("--split")
        .flag(default = false)
        .help("Whether to split the output in multiple JSON files (default: false)")

    private val comparatorsNames: Set<String> by option("--comparator")
        .multiple(default = comparators.keys.toList())
        .unique()
        .help("The comparator to use")

    private val filters: Map<String, String> by option("-F", "--filter")
        .associate()
        .help("The case-sensitive filters to apply to the correspondences list (e.g. --filter column1=value1)")

    private val comparatorsParams: Map<String, Any> by option("-P", "--comparator-param")
        .associate()
        .help("The parameters to pass to the comparators in the format comp1.param1=value")

    private val maxValueLen by option("-L", "--max-value-len")
        .int()
        .default(100)
        .help("The maximum length of the value in the diff output (default: 100)")

    private val parallel by option("--parallel")
        .flag("--no-parallel", default = false)
        .help("Whether to compare images in parallel. WARNING: This may consume a lot of memory and potentially crash the program. (default: false)")

    private val truncate by option("--truncate")
        .flag("--no-truncate", default = true)
        .help("Whether to truncate the diff values in the output to the maximum length (default: true)")

    override fun run() {
        try {
            val diffResults = if (coverImagePath.isDirectory() && stegoImagePath.isDirectory()) {
                val coverImages = coverImagePath.gatherFiles()
                val stegoImages = stegoImagePath.gatherFiles()

                logger.info("Creating pairs of images to compare...")
                val pairs = createPairs(coverImages, stegoImages).toList()
                logger.info("Found ${pairs.size} pairs of images")

                progressBarBuilder.setInitialMax(pairs.size.toLong()).build().use { progressBar ->
                    val doCompare = { (first, second): Pair<Path, Path> ->
                        val res = compare(
                            Image(first),
                            Image(second),
                        )
                        progressBar.step()
                        res
                    }

                    if (parallel) {
                        logger.info("Comparing images in parallel...")
                        runBlocking(coroutineContext) {
                            pairs.pmap(doCompare)
                        }
                    } else {
                        pairs.map(doCompare).asFlow()
                    }
                }
            } else {
                val diffRes = compare(
                    Image(coverImagePath),
                    Image(stegoImagePath),
                )
                flowOf(diffRes)
            }

            logger.info("Writing output...")
            runBlocking(coroutineContext) {
                writeOutput(diffResults.toList())
            }
            logger.info("Done")
        } catch (e: ImageDiffException) {
            logger.error(e.message)
        } catch (e: IOException) {
            logger.error("An I/O error occurred: ${e.message}")
            e.printStackTrace(System.err)
        } catch (e: Exception) {
            logger.error("An unknown error occurred: ${e.message}")
            e.printStackTrace(System.err)
        }
    }

    private fun createPairs(coverImages: Sequence<Path>, stegoImages: Sequence<Path>): Sequence<Pair<Path, Path>> {
        return corListPath?.inputStream()?.let { csvInputStream ->
            csvReader.readAllWithHeader(csvInputStream)
                .asSequence()
                .filter { map ->
                    filters.all { (key, value) -> map[key] == value }
                }
                .map { map ->
                    val stego = stegoImagePath / (map["stego_image_filename"]
                        ?: throw MalformedCorrespondenceListException("stego_image_filename"))
                    val cover = coverImagePath / (map["cover_image_filename"]
                        ?: throw MalformedCorrespondenceListException("cover_image_filename"))
                    cover to stego
                }
        } ?: run {
            logger.warn("No correspondence list provided. Comparing images in the order they are found in the directories.")
            coverImages.zip(stegoImages)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeOutput(diffResults: Iterable<DiffResult>) {
        val statistics = Statistics(
            diffResults.computeRateOfChanges(),
            diffResults.computeChangesPerImage()
        )

        val optimizedDiffResults = diffResults.optimizeForOutput()
        if (splitOutput) {
            val dir = output.createDirectories()
            optimizedDiffResults.forEach { diffRes ->
                json.encodeToStream(diffRes, diffRes.outputStream(dir))
            }
        } else {
            val diffData = DiffData("1.0", System.currentTimeMillis(), statistics, optimizedDiffResults.toList())
            json.encodeToStream(diffData, output.outputStream())
        }
    }

    private fun Iterable<DiffResult>.optimizeForOutput(): List<DiffResult> {
        // Skip optimization if none of the flags are set
        if (!truncate) return toList()

        return map { diffRes ->
            diffRes.copy(
                diff = diffRes.diff.mapValues { (_, diffValueMap) ->
                    diffValueMap.mapValues { (_, value) ->
                        var newValue = value
                        // This is used here to allow for easy addition of further optimizations in the future
                        if (truncate) {
                            newValue = newValue.copy(
                                cover = newValue.cover?.truncate(),
                                stego = newValue.stego?.truncate(),
                                diffv = newValue.diffv?.truncate(),
                            )
                        }
                        newValue
                    }
                }
            )
        }
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

    /**
     * Compare two images using the specified comparators.
     * @param first The first image to compare
     * @param second The second image to compare
     * @return The [DiffResult] of the comparison
     */
    private fun compare(first: Image, second: Image): DiffResult {
        return comparatorsNames
            .mapNotNull { compName ->
                val pair = comparators[compName] to comparatorsParams
                    .filter { (k, _) -> k.startsWith("$compName.") }
                    .mapKeys { (k, _) -> k.removePrefix("$compName.") }
                pair.takeIf { it.first != null }
            }
            .map { (comp, args) -> comp!!.compare(first, second, args) }
            .reduce { acc, diff -> acc + diff }
    }
}
