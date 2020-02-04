package io.newblack.elastic.test

import io.newblack.elastic.FlexibleSynonymTokenFilterFactory
import io.newblack.elastic.SynonymResource
import io.newblack.elastic.SynonymResourceFactory
import io.newblack.elastic.SynonymWatcher
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.synonym.SynonymMap
import org.apache.lucene.util.CharsRef
import org.elasticsearch.Version
import org.elasticsearch.cluster.metadata.IndexMetaData
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.SettingsException
import org.elasticsearch.index.IndexSettings
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*

class FlexibleSynonymTokenFilterFactoryTests {

    private val synonymResourceFactory = mock(SynonymResourceFactory::class.java)
    private val synonymWatcher = mock(SynonymWatcher::class.java)

    private fun createFactory(settings: Settings): FlexibleSynonymTokenFilterFactory {
        return FlexibleSynonymTokenFilterFactory(
                IndexSettings(
                        IndexMetaData.builder("my_index")
                                .settings(
                                        Settings.builder()
                                                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 3)
                                                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                                                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.V_6_8_4)
                                                .build()
                                )
                                .build(),
                        Settings.builder()
                                .build()
                ),
                "flexible_synonym",
                settings,
                synonymResourceFactory,
                synonymWatcher
        )
    }

    @Test
    fun shouldStartWatchingFilterAndResource() {
        val resource = mock(SynonymResource::class.java)

        `when`(resource.load()).thenReturn(
                SynonymMap.Builder().apply {
                    add(CharsRef("med"), CharsRef("medicine"), true)
                }
                        .build()
        )

        `when`(synonymResourceFactory.create(
                anyString(),
                anyString()
        )).thenReturn(resource)

        createFactory(
                Settings.builder()
                        .put(FlexibleSynonymTokenFilterFactory.SYNONYM_BASE_URL.key, "http://localhost")
                        .put(FlexibleSynonymTokenFilterFactory.SYNONYM_URI.key, "static-resource")
                        .build()
        )
                .create(WhitespaceTokenizer())

        verify(synonymWatcher, times(1)).startWatching(
                any(),
                any(),
                any()
        )
    }

    private fun <T> any(): T {
        return ArgumentMatchers.any<T>()
    }

}