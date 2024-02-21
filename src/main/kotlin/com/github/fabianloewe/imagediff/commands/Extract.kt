package com.github.fabianloewe.imagediff.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.fabianloewe.imagediff.*
import kotlinx.coroutines.flow.*
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
    private val extractors: Map<String, ImageExtractor>,
) : CliktCommand(), KoinScopeComponent {
    override val scope by newScope()

    private val progressBarBuilder: ProgressBarBuilder by inject()
    private val coroutineContext: CoroutineContext by inject()
    private val csvReader: CsvReader by inject()

    private val stegoImagePath by option("-s", "--stego")
        .path()
        .required()
        .help("The second file or directory containing images to compare")

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
        .multiple(default = extractors.keys.toList())
        .unique()
        .help("The comparator to use")

    private val filters: Map<String, String> by option("-F", "--filter")
        .associate()
        .help("The case-sensitive filters to apply to the correspondences list on a certain column (e.g. --filter column1=value1)")

    private val extractorsParams: Map<String, Any> by option("-P", "--extractor-param")
        .associate()
        .help("The parameters to pass to the extractors in the format extr1.param1=value")

    private val parallel by option("--parallel")
        .flag("--no-parallel", default = false)
        .help("Whether to extract from images in parallel. WARNING: This may consume a lot of memory and potentially crash the program. (default: false)")

    override fun run() {
        try {
            val extractionResults: Flow<Pair<Image, ExtractedData>> = if (stegoImagePath.isDirectory()) {
                val stegoImages = collectFiles()

                val stegoImagesCount = stegoImages.count().toLong()
                logger.info("Extracting from $stegoImagesCount images...")

                progressBarBuilder.setInitialMax(stegoImagesCount).build().use { progressBar ->
                    val doExtract = { path: Path ->
                        val image = Image(path)
                        val extractedData = extract(image)
                        progressBar.step()
                        image to extractedData
                    }

                    if (parallel) {
                        logger.info("Extracting in parallel...")
                        runBlocking(coroutineContext) {
                            stegoImages.pmap(doExtract)
                        }
                    } else {
                        stegoImages.toList().map(doExtract).asFlow()
                    }
                }
            } else {
                val stegoImage = Image(stegoImagePath)
                flowOf(stegoImage to extract(stegoImage))
            }

            runBlocking {
                extractionResults
                    .onStart {
                        output.createDirectories()
                    }
                    .onCompletion {
                        logger.info("Done")
                    }
                    .collect { (image, extractedData) ->
                        writeOutput(image, extractedData)
                    }
            }
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

    private fun extract(image: Image): ExtractedData {
        return extractorsNames.associateWith { name ->
            val extractor = extractors[name] ?: throw UnknownImageExtractorException(name)
            val argsMap = extractorsParams.filterKeys { it.startsWith("$name.") }
                .mapKeys { it.key.removePrefix("$name.") }
            extractor.extract(image, argsMap)
        }
    }

    private fun writeOutput(image: Image, extractedData: ExtractedData) {
        extractedData.forEach { (extractorName, extractorData) ->
            val extractorOutput = output / "${image.path.nameWithoutExtension}_$extractorName.bin"
            extractorOutput.outputStream().use {
                it.write(extractorData)
            }
        }
    }
}