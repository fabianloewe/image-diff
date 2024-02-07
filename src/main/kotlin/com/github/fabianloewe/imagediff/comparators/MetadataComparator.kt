package com.github.fabianloewe.imagediff.comparators

import com.github.fabianloewe.imagediff.*
import com.sksamuel.scrimage.metadata.ImageMetadata
import java.io.InputStream

class MetadataComparator : ImageComparator {
    override fun compare(first: Image, second: Image): DiffResult {
        val firstMetadata = extractMetadata(first.inputStream)
        val secondMetadata = extractMetadata(second.inputStream)

        val metadataDiff = firstMetadata
            .filter { (key, value) -> secondMetadata[key] != value }
            .mapValues { (key, value) -> DiffValue(value, secondMetadata[key]) }
        return DiffResult(first, second, mapOf(NAME to metadataDiff))
    }

    private fun extractMetadata(inputStream: InputStream): Map<DiffKey, String> {
        val metadata = ImageMetadata.fromStream(inputStream)
        return metadata.tags().associate { DiffKey(it.name) to it.value }
    }

    companion object {
        const val NAME = "metadata"
    }
}