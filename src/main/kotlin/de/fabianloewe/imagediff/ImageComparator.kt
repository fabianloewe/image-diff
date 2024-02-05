package de.fabianloewe.imagediff

import java.io.InputStream

typealias Diff = Map<String, Pair<String, String>>

interface ImageComparator {
    fun compare(first: InputStream, second: InputStream): Diff
}