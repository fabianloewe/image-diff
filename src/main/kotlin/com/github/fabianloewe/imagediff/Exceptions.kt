package com.github.fabianloewe.imagediff

abstract class ImageDiffException(
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnknownImageComparatorException(
    comparatorName: String,
) : ImageDiffException("The image comparator '$comparatorName' is unknown")

class MalformedCorrespondenceListException(
    missingColumn: String,
) : ImageDiffException("The correspondence list is malformed. The following column is missing: $missingColumn")

abstract class ImageExtractorException(
    override val message: String,
    cause: Throwable? = null,
) : ImageDiffException(message, cause)

class UnknownImageExtractorException(
    extractorName: String,
) : ImageExtractorException("The image extractor '$extractorName' is unknown")