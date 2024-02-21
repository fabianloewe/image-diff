package com.github.fabianloewe.imagediff

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.fabianloewe.imagediff.commands.Diff
import com.github.fabianloewe.imagediff.commands.Extract
import com.github.fabianloewe.imagediff.comparators.CompositeComparator
import com.github.fabianloewe.imagediff.comparators.MetadataComparator
import com.github.fabianloewe.imagediff.extractors.LeastSignificantBitsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import me.tongfei.progressbar.DelegatingProgressBarConsumer
import me.tongfei.progressbar.ProgressBarBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

class Main : CliktCommand(name = "image-diff"), KoinComponent {
    private val diffCmd: Diff by inject()
    private val extractCmd: Extract by inject()

    override fun run() = Unit

    fun run(args: Array<String>) {
        subcommands(diffCmd, extractCmd).main(args)
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val appModule = module {
        single(named("comparators")) {
            mapOf(
                MetadataComparator.NAME to MetadataComparator(),
                CompositeComparator.NAME to CompositeComparator(),
            )
        }

        single(named("extractors")) {
            mapOf(
                LeastSignificantBitsExtractor.NAME to LeastSignificantBitsExtractor()
            )
        }

        single {
            Dispatchers.IO.limitedParallelism(
                Runtime.getRuntime().availableProcessors() * 2
            )
        } bind CoroutineContext::class

        single {
            Json {
                prettyPrint = true
                explicitNulls = false
            }
        }

        single { csvReader() }

        single {
            ProgressBarBuilder()
                .setTaskName("Comparing images")
                .setConsumer(DelegatingProgressBarConsumer(logger::info))
        }

        single { Diff(get(named("comparators"))) }

        single { Extract(get(named("extractors"))) }
    }
    startKoin {
        printLogger(Level.INFO)

        modules(appModule)

        Main().run(args)
    }
}