package com.github.fabianloewe.imagediff.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.fabianloewe.imagediff.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
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
    allComparators: List<ImageComparator>,
) : CliktCommand(), KoinScopeComponent {
    override val scope: Scope by newScope()

    private val progressBarBuilder: ProgressBarBuilder by inject()
    private val coroutineContext: CoroutineContext by inject()
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

    private val comparatorsNames: Set<String> by option("--comparator")
        .multiple(default = allComparators.map { it.name })
        .unique()
        .help("The comparator to use")

    private val filters: Map<String, String> by option("-F", "--filter")
        .associate()
        .help("The case-sensitive filters to apply to the correspondences list (e.g. --filter column1=value1)")

    private val parallel by option("--parallel")
        .flag("--no-parallel", default = false)
        .help("Whether to compare images in parallel. WARNING: This may consume a lot of memory and potentially crash the program. (default: false)")

    private val comparators by lazy {
        comparatorsNames.map { name ->
            allComparators.find { it.name == name }
                ?: throw UnknownImageComparatorException(name)
        }
    }

    init {
        allComparators.forEach {
            this.registerOptionGroup(it.args)
            it.args.all.forEach { opt -> registerOption(opt) }
        }
    }

    override fun run() = runBlocking(coroutineContext) {
        try {
            val coverImages = if (coverImagePath.isDirectory()) {
                coverImagePath.gatherFiles()
            } else {
                sequenceOf(coverImagePath)
            }
            val stegoImages = if (stegoImagePath.isDirectory()) {
                stegoImagePath.gatherFiles()
            } else {
                sequenceOf(stegoImagePath)
            }

            logger.info("Creating pairs of images to compare...")
            val pairs = createPairs(coverImages, stegoImages)
            val pairsCount = pairs.count().toLong()
            logger.info("Found $pairsCount pairs of images")

            val progressBar = progressBarBuilder.setInitialMax(pairsCount).build()
            pairs
                .let {
                    val doCompare = { (first, second): Pair<Path, Path> ->
                        compare(
                            Image(first),
                            Image(second),
                        )
                    }

                    if (parallel) {
                        logger.info("Comparing images in parallel...")
                        it.pmap(doCompare)
                    } else {
                        logger.info("Comparing images sequentially...")
                        it.map(doCompare).asFlow()
                    }
                }
                .withProgressBar(progressBar)
                .writeOutput()
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

    /**
     * Compare two images using the specified comparators.
     * @param first The first image to compare
     * @param second The second image to compare
     * @return The results of the comparison
     */
    private fun compare(first: Image, second: Image): List<Result<ImageComparisonData>> {
        return comparatorsNames
            .map { name -> comparators.find { it.name == name } ?: throw UnknownImageComparatorException(name) }
            .map { comp -> comp.compare(first, second) }
    }

    private suspend fun Flow<List<Result<ImageComparisonData>>>.writeOutput() {
        val outputDirPerComparator = comparators.associate { comp ->
            val dir = output / "diff" / comp.name
            comp.name to dir.createDirectories()
        }

        this
            .onStart {
                logger.info("Writing output...")
            }
            .onCompletion {
                logger.info("Done")
            }
            .collect { results ->
                for (result in results) {
                    result
                        .onSuccess { data ->
                            val dir = outputDirPerComparator[data.comparator.name]!!
                            val path = dir / nameResultFileName(
                                data.coverImage,
                                data.stegoImage,
                                data.diffFileExtension
                            )
                            data.diffOutput.writeTo(path.outputStream())
                        }
                        .onFailure { e ->
                            logger.error("An error occurred: ${e.message}")
                            e.printStackTrace(System.err)
                        }
                }
            }
    }
}

