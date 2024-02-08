package com.github.fabianloewe.imagediff

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import me.tongfei.progressbar.ProgressBarBuilder
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.newScope
import org.koin.core.scope.Scope
import java.io.IOException
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*

class DiffCommand(
    private val comparators: Map<String, ImageComparator>,
    private val coroutineContext: CoroutineContext,
    private val json: Json,
    private val csvReader: CsvReader,
    private val progressBarBuilder: ProgressBarBuilder,
) : CliktCommand(), KoinScopeComponent {
    override val scope: Scope by newScope()

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
        .help("Whether to split the output in multiple JSON files")

    private val comparatorsNames: Set<String> by option("--comparator")
        .multiple(default = comparators.keys.toList())
        .unique()
        .help("The comparator to use")

    private val filters: Map<String, String> by option("-F", "--filter")
        .associate()
        .help("The case-sensitive filters to apply to the correspondences list (e.g. --filter column1=value1)")

    private val ignoreNulls by option("--ignore-nulls")
        .flag("--no-ignore-nulls", default = true)
        .help("Whether to ignore null values for stego images in the diff output")

    override fun run() {
        try {
            val diffResults = if (coverImagePath.isDirectory() && stegoImagePath.isDirectory()) {
                val coverImages = coverImagePath.gatherFiles()
                val stegoImages = stegoImagePath.gatherFiles()

                logger.info("Creating pairs of images to compare...")
                val pairs = createPairs(coverImages, stegoImages)
                logger.info("Found ${pairs.size} pairs of images")

                progressBarBuilder.setInitialMax(pairs.size.toLong()).build().use { progressBar ->
                    runBlocking(coroutineContext) {
                        pairs.pmap { (first, second) ->
                            val res = compare(
                                Image(first, first.inputStream()),
                                Image(second, second.inputStream()),
                                ignoreNulls
                            )
                            progressBar.step()
                            res
                        }
                    }
                }
            } else {
                val diffRes = compare(
                    Image(coverImagePath, coverImagePath.inputStream()),
                    Image(stegoImagePath, stegoImagePath.inputStream()),
                    ignoreNulls
                )
                listOf(diffRes)
            }

            logger.info("Writing output...")
            writeOutput(diffResults)
            logger.info("Done")
        } catch (e: ImageDiffException) {
            logger.error(e.message)
        } catch (e: IOException) {
            logger.error("An I/O error occurred: ${e.cause?.message ?: e.message}")
        } catch (e: Exception) {
            logger.error("An unknown error occurred: ${e.message}")
        }
    }

    private fun createPairs(coverImages: List<Path>, stegoImages: List<Path>): List<Pair<Path, Path>> {
        return corListPath?.inputStream()?.let { csvInputStream ->
            csvReader.readAllWithHeader(csvInputStream)
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
    private fun writeOutput(diffResults: List<DiffResult>) {
        val statistics = Statistics(
            diffResults.computeRateOfChanges(),
            diffResults.computeChangesPerImage()
        )
        if (splitOutput) {
            val dir = output.createDirectories()
            for (diffRes in diffResults) {
                json.encodeToStream(diffRes, diffRes.outputStream(dir))
            }
        } else {
            val diffData = DiffData("1.0", System.currentTimeMillis(), statistics, diffResults)
            json.encodeToStream(diffData, output.outputStream())
        }
    }

    /**
     * Compare two images using the specified comparators.
     * @param first The first image to compare
     * @param second The second image to compare
     * @param ignoreNulls Whether to ignore null values for stego images in the [DiffResult]
     * @return The [DiffResult] of the comparison
     */
    private fun compare(first: Image, second: Image, ignoreNulls: Boolean): DiffResult {
        return comparatorsNames
            .mapNotNull { comparators[it] }
            .map { it.compare(first, second, ignoreNulls) }
            .reduce { acc, diff -> acc + diff }
    }
}
