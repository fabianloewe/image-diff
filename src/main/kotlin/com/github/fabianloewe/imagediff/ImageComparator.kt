package com.github.fabianloewe.imagediff

interface ImageComparator {
    fun compare(first: Image, second: Image): DiffResult
}