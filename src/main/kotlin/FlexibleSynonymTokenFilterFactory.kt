package io.newblack.elastic

import org.apache.lucene.analysis.TokenStream
import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.SettingsException
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory
import java.net.URL

class FlexibleSynonymTokenFilterFactory(
        indexSettings: IndexSettings,
        name: String,
        private val settings: Settings,
        private val synonymResourceFactory: SynonymResourceFactory,
        private val synonymWatcher: SynonymWatcher
) : AbstractTokenFilterFactory(indexSettings, name, settings) {

    companion object {
        val SYNONYM_BASE_URL = Setting.simpleString("synonyms_base")
        val SYNONYM_PATH = Setting.simpleString("synonyms_path")
        val SYNONYM_URI = Setting.simpleString("synonyms_uri")
        val SYNONYM_FORMAT = Setting.simpleString("synonyms_format")
    }

    // Invoked the first x times and then the instances are cached by Elastic
    override fun create(tokenStream: TokenStream): TokenStream {
        var target = SYNONYM_URI.get(settings).ifBlank {
            val baseUrl = SYNONYM_BASE_URL.get(settings).ifBlank {
                System.getenv("SYNONYM_BASE_URL") ?: throw SettingsException("missing base url")
            }

            val path = SYNONYM_PATH.get(settings)

            URL(URL(baseUrl), path).toString()
        }

        val resource = synonymResourceFactory.create(target, SYNONYM_FORMAT.get(settings))
        val filter = DynamicSynonymFilter(tokenStream, resource.load())

        synonymWatcher.startWatching(indexSettings.index, filter, resource)

        return filter
    }

}