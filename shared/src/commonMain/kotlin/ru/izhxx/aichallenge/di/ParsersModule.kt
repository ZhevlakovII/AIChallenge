package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.data.parser.core.JsonParser
import ru.izhxx.aichallenge.data.parser.core.MarkdownParser
import ru.izhxx.aichallenge.data.parser.core.ResultParser
import ru.izhxx.aichallenge.data.parser.impl.JsonParserImpl
import ru.izhxx.aichallenge.data.parser.impl.MarkdownParserImpl
import ru.izhxx.aichallenge.data.parser.impl.ResultParserImpl

val parsersModule = module {
    single<MarkdownParser> { MarkdownParserImpl() }
    single<JsonParser> { JsonParserImpl(markdownParser = get(), json = get()) }
    single<ResultParser> { ResultParserImpl(jsonParser = get(), markdownParser = get()) }
}