package de.fabianloewe.imagediff

import de.fabianloewe.imagediff.comparators.MetadataComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val comparators = mapOf(
        "metadata" to MetadataComparator(),
    )
    DiffCommand(
        comparators,
        Dispatchers.Default,
        Json { prettyPrint = true }
    ).main(args)
}