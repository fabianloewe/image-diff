package com.github.fabianloewe.imagediff.comparators.metadata

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
@JvmInline
value class DiffKey(private val value: String) : Comparable<DiffKey> {
    override fun compareTo(other: DiffKey): Int {
        return value.compareTo(other.value)
    }
}

@Serializable
data class DiffValue(
    val cover: JsonElement?,
    val stego: JsonElement?,
)

typealias Diff = Map<DiffKey, DiffValue>

@Serializable
data class DiffResult(val coverPath: String, val stegoPath: String, val diff: Diff)