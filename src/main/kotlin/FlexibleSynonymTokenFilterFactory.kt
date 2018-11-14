package io.newblack.elastic

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.SettingsException
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory
import java.net.URI

class FlexibleSynonymTokenFilterFactory(
        indexSettings: IndexSettings,
        name: String,
        private val settings: Settings,
        private val synonymWatcher: FlexibleSynonymWatcher
) : AbstractTokenFilterFactory(indexSettings, name, settings) {

    private val SYNONYM_URI = Setting.simpleString("synonyms_uri")
    private val SYNONYM_FORMAT = Setting.simpleString("synonyms_format")

    // Invoked the first x times and then the instances are cached by Elastic
    override fun create(tokenStream: TokenStream): TokenStream {
        logger.info(
                "created called in FlexibleSynonymTokenFilterFactory, uri={}",
                SYNONYM_URI.get(settings)
        )

        val uri = SYNONYM_URI.get(settings)
        if (uri.isNullOrEmpty()) {
            throw SettingsException("setting ${SYNONYM_URI.key} is required")
        }

        val format = SYNONYM_FORMAT.get(settings)

        // TODO(kevin): let this be configurable based on settings
        // TODO(kevin): grab an exisiting analyzer
        val analyzer = object : Analyzer() {
            override fun createComponents(fieldName: String?): TokenStreamComponents {
                val tokenizer = WhitespaceTokenizer()
                return TokenStreamComponents(tokenizer, LowerCaseFilter(tokenizer))
            }
        }

        val resourceUri = URI(uri)

        // TODO(kevin): support for multiple types of resources
        val resource = if (resourceUri.scheme.startsWith("http")) {
            logger.info("using WebSynonymResource for {}", uri)
            WebSynonymResource(true, analyzer, format, uri)
        } else {
            logger.info("using LocalSynonymResource for {}", uri)
            LocalSynonymResource(true, analyzer, format, uri)
        }

        val filter = DynamicSynonymFilter(tokenStream, resource.load())

        synonymWatcher.watch(indexSettings.index, filter, resource)

        return filter
    }

}