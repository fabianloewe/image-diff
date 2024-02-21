package com.github.fabianloewe.imagediff

typealias ImageExtractorArgsMap = Map<String, Any>

/**
 * Interface for extracting data from an image.
 */
interface ImageExtractor {
    /**
     * Extracts data from an image.
     *
     * @param image The image to extract data from.
     * @param argsMap Additional arguments for the extraction.
     * @return The extracted data.
     */
    fun extract(image: Image, argsMap: ImageExtractorArgsMap): ByteArray
}
