package com.github.fabianloewe.imagediff

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.fabianloewe.imagediff.comparators.CompositeComparator
import com.github.fabianloewe.imagediff.comparators.MetadataComparator
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

class Main : KoinComponent {
    private val diffCmd: DiffCommand by inject()

    fun main(args: Array<String>) {
        diffCmd.main(args)
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val appModule = module {
        single(named("comparators")) {
            mapOf(
                MetadataComparator.NAME to MetadataComparator(),
                CompositeComparator.NAME to CompositeComparator()
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

        single { DiffCommand(get(named("comparators")), get(), get(), get(), get()) }
    }
    startKoin {
        printLogger(Level.INFO)

        modules(appModule)

        Main().main(args)
    }
}