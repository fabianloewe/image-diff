package de.fabianloewe.imagediff.comparators

import com.sksamuel.scrimage.metadata.ImageMetadata
import de.fabianloewe.imagediff.Diff
import de.fabianloewe.imagediff.ImageComparator
import java.io.InputStream

class MetadataComparator : ImageComparator {
    override fun compare(first: InputStream, second: InputStream): Diff {
        val firstMetadata = extractMetadata(first)
        val secondMetadata = extractMetadata(second)

        return firstMetadata
            .filter { (key, value) -> secondMetadata[key] != value }
            .mapValues { (key, value) -> value to secondMetadata[key]!! }
    }

    private fun extractMetadata(inputStream: InputStream): Map<String, String> {
        val metadata = ImageMetadata.fromStream(inputStream)
        return metadata.tags().associate { it.name to it.value }
    }
}