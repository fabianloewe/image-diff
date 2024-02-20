package com.github.fabianloewe.imagediff

fun Iterable<DiffResult>.computeChangesPerImage(): Map<DiffResultId, Int> {
    return this.fold(emptyMap()) { acc, diffRes ->
        acc + (diffRes.id to diffRes.diff.values.sumOf { it.size })
    }
}

fun Iterable<DiffResult>.computeRateOfChanges(): Map<String, Map<DiffKey, Float>> {
    val totalChangesPerKey = this
        .map { diffRes -> diffRes.diff }
        .fold(emptyMap<String, Map<DiffKey, Int>>()) { acc, compMap ->
            acc + compMap.mapValues { (compName, map) ->
                val accMap = acc[compName] ?: emptyMap()
                accMap + map.mapValues { (key, _) ->
                    accMap.getOrDefault(key, 0) + 1
                }
            }
        }

    val size = count().toFloat()
    return totalChangesPerKey.mapValues { (_, map) ->
        map.mapValues { (_, count) ->
            count / size
        }.toSortedMap()
    }.toSortedMap()
}