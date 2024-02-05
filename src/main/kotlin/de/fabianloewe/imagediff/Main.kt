package de.fabianloewe.imagediff

import de.fabianloewe.imagediff.comparators.MetadataComparator
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    val comparators = mapOf(
        "metadata" to MetadataComparator(),
    )
    DiffCommand(comparators, Dispatchers.Default).main(args)
}