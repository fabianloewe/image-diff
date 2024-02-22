package com.github.fabianloewe.imagediff

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.groups.OptionGroup

/**
 * Interface for the options of an image processor.
 */
interface ImageProcessorArgs {
    /**
     * All options of the image processor.
     */
    val all: List<GroupableOption>
}

/**
 * Interface for processing images.
 */
interface ImageProcessor<Args : OptionGroup> {
    /**
     * The name of the image processor.
     */
    val name: String

    /**
     * The options for the image processor.
     */
    val args: Args
}