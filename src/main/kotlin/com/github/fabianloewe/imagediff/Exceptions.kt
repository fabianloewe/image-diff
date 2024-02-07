package com.github.fabianloewe.imagediff

abstract class ImageDiffException(
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class MalformedCorrespondenceListException(
    missingColumn: String,
) : ImageDiffException("The correspondence list is malformed. The following column is missing: $missingColumn")