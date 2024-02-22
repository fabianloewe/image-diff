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
import java.io.IOException
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*

class Extract(
    allExtractors: List<ImageExtractor>,
) : CliktCommand(), KoinScopeComponent {
    override val scope by newScope()

    private val progressBarBuilder: ProgressBarBuilder by inject()
    private val coroutineContext: CoroutineContext by inject()
    private val csvReader: CsvReader by inject()

    private val stegoImagePath by option("-s", "--stego")
        .path()
        .required()
        .help("The file or directory containing images to compare")

    private val output by option("-o", "--output")
        .path()
        .required()
        .help(
            """
            The directory to write the extracted data to. 
            There will be one plain file per image per extractor and a JSON file containing paths to the extracted data.
            """.trimIndent()
        )

    private val corListPath by option("-l", "--cor-list")
        .path()
        .help(
            """
            The file containing correspondences between the images as a CSV file.
             Only one column named stego_image_filename must be present.
             This can be handy with the -F option to filter the correspondences list.
            """.trimIndent()
        )

    private val extractorsNames: Set<String> by option("--extractor")
        .multiple(default = allExtractors.map { it.name })
        .unique()
        .help("The comparator to use")

    private val filters: Map<String, String> by option("-F", "--filter")
        .associate()
        .help("The case-sensitive filters to apply to the correspondences list on a certain column (e.g. --filter column1=value1)")

    private val parallel by option("--parallel")
        .flag("--no-parallel", default = false)
        .help("Whether to extract from images in parallel. WARNING: This may consume a lot of memory and potentially crash the program. (default: false)")

    private val extractors by lazy {
        extractorsNames.map { name ->
            allExtractors.find { it.name == name }
                ?: throw UnknownImageExtractorException(name)
        }
    }

    init {
        allExtractors.forEach {
            this.registerOptionGroup(it.args)
            it.args.all.forEach { opt -> registerOption(opt) }
        }
    }

    override fun run() = runBlocking(coroutineContext) {
        try {
            val stegoImages = if (stegoImagePath.isDirectory()) {
                collectFiles()
            } else {
                sequenceOf(stegoImagePath)
            }

            val stegoImagesCount = stegoImages.count().toLong()
            logger.info("Extracting from $stegoImagesCount image${if (stegoImagesCount == 1L) "" else "s"}...")

            val progressBar = progressBarBuilder.setInitialMax(stegoImagesCount).build()
            stegoImages
                .map { path -> Image(path) }
                .let {
                    if (parallel) {
                        logger.info("Extracting in parallel...")
                        it.pmap(::extract)
                    } else {
                        logger.info("Extracting sequentially...")
                        it.map(::extract).asFlow()
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

    private fun collectFiles(): Sequence<Path> {
        return corListPath?.inputStream()?.let { csvInputStream ->
            csvReader.readAllWithHeader(csvInputStream)
                .asSequence()
                .filter { map ->
                    filters.all { (key, value) -> map[key] == value }
                }
                .map { map ->
                    stegoImagePath / (map["stego_image_filename"]
                        ?: throw MalformedCorrespondenceListException("stego_image_filename"))
                }
        } ?: stegoImagePath.gatherFiles()
    }

    private fun extract(image: Image): List<Result<ImageExtractionData>> {
        return extractors.map { extractor ->
            extractor.extract(image)
        }
    }

    private suspend fun Flow<List<Result<ImageExtractionData>>>.writeOutput() {
        val outputDirPerComparator = extractors.associate { extract ->
            val dir = output / "extract" / extract.name
            extract.name to dir.createDirectories()
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
                            val dir = outputDirPerComparator[data.extractor.name]!!
                            val path = dir / nameResultFileName(
                                data.image,
                                data.extractFileExtension,
                            )
                            data.extractOutput.writeTo(path.outputStream())
                        }
                        .onFailure { e ->
                            logger.error("An error occurred: ${e.message}")
                            e.printStackTrace(System.err)
                        }
                }
            }
    }
}
