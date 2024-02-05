package de.fabianloewe.imagediff.comparators

import com.sksamuel.scrimage.metadata.ImageMetadata
import de.fabianloewe.imagediff.DiffResult
import de.fabianloewe.imagediff.Image
import de.fabianloewe.imagediff.ImageComparator
import java.io.InputStream

class MetadataComparator : ImageComparator {
    override fun compare(first: Image, second: Image): DiffResult {
        val firstMetadata = extractMetadata(first.inputStream)
        val secondMetadata = extractMetadata(second.inputStream)

        val diff = firstMetadata
            .filter { (key, value) -> secondMetadata[key] != value }
            .mapValues { (key, value) -> value to secondMetadata[key]!! }
        return DiffResult(first, second, diff)
    }

    private fun extractMetadata(inputStream: InputStream): Map<String, String> {
        val metadata = ImageMetadata.fromStream(inputStream)
        return metadata.tags().associate { it.name to it.value }
    }
}