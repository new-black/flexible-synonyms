package io.newblack.elastic

import org.elasticsearch.client.Client
import org.elasticsearch.cluster.service.ClusterService
import org.elasticsearch.common.logging.ESLoggerFactory
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.index.Index
import org.elasticsearch.index.IndexModule
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.index.analysis.TokenFilterFactory
import org.elasticsearch.index.shard.IndexEventListener
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider
import org.elasticsearch.indices.cluster.IndicesClusterStateService
import org.elasticsearch.plugins.AnalysisPlugin
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.script.ScriptService
import org.elasticsearch.threadpool.ThreadPool
import org.elasticsearch.watcher.ResourceWatcherService

class FlexibleSynonymsPlugin : Plugin(), AnalysisPlugin {

    private val logger = ESLoggerFactory.getLogger(FlexibleSynonymsPlugin::class.java)

    private lateinit var watcher: ScheduledSynonymWatcher

    override fun onIndexModule(indexModule: IndexModule) {
        indexModule.addIndexEventListener(object : IndexEventListener {
            override fun afterIndexRemoved(
                    index: Index,
                    indexSettings: IndexSettings,
                    reason: IndicesClusterStateService.AllocatedIndices.IndexRemovalReason
            ) {
                logger.info("after index removed called {} because {}", index.name, reason.name)

                watcher.stopWatching(index)
            }
        })
    }

    override fun createComponents(
            client: Client,
            clusterService: ClusterService,
            threadPool: ThreadPool,
            resourceWatcherService: ResourceWatcherService,
            scriptService: ScriptService,
            xContentRegistry: NamedXContentRegistry
    ): MutableCollection<Any> {
        watcher = ScheduledSynonymWatcher(clusterService.settings, threadPool.scheduler())

        return mutableListOf(watcher)
    }

    override fun getTokenFilters() = mutableMapOf(
            "flexible_synonym" to AnalysisProvider<TokenFilterFactory> { indexSettings, _, name, settings ->
                FlexibleSynonymTokenFilterFactory(indexSettings, name, settings, DefaultSynonymResourceFactory(), watcher)
            }
    )
}
