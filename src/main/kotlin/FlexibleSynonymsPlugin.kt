package io.newblack.elastic

import org.elasticsearch.client.Client
import org.elasticsearch.cluster.service.ClusterService
import org.elasticsearch.common.component.AbstractLifecycleComponent
import org.elasticsearch.common.logging.ESLoggerFactory
import org.elasticsearch.common.settings.Settings
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class FlexibleSynonymsPlugin : Plugin(), AnalysisPlugin {

    private val logger = ESLoggerFactory.getLogger(FlexibleSynonymsPlugin::class.java)

    private lateinit var watcher: FlexibleSynonymWatcher

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
        logger.info("create components called")

        watcher = FlexibleSynonymWatcher(clusterService.settings, threadPool.scheduler())

        return mutableListOf(watcher)
    }

    override fun getTokenFilters() = mutableMapOf(
        "flexible_synonym" to AnalysisProvider<TokenFilterFactory> {
                indexSettings, _, name, settings -> FlexibleSynonymTokenFilterFactory(indexSettings, name, settings, watcher)
            }
        )
}

class FlexibleSynonymWatcher(
        settings: Settings,
        private val scheduler: ScheduledExecutorService
) : AbstractLifecycleComponent(settings) {

    private val filters: MutableMap<String, MutableCollection<Pair<DynamicSynonymFilter, SynonymResource>>> = ConcurrentHashMap()

    private var schedule: ScheduledFuture<*>? = null

    fun watch(index: Index, filter: DynamicSynonymFilter, resource: SynonymResource) {
        logger.info("start watching filter/resource for index {}", index.name)
        filters.getOrPut(index.uuid, ::mutableListOf).add(Pair(filter, resource))
    }

    fun stopWatching(index: Index) {
        logger.info("stop watching all filters/resources for index {}", index.name)
        filters.remove(index.uuid)
    }

    override fun doStart() {
        schedule = scheduler.scheduleAtFixedRate({
            logger.info("updating resources of {} filters..", filters.count())

            filters.forEach { it ->
                it.value.forEach {
                    logger.info("checking if reload is required..")
                    if (it.second.needsReload()) {
                        logger.info("reload is required")

                        val m = it.second.load()

                        logger.info("loaded synonyms: {}, {}", m, m.fst)

                        it.first.update(m)
                    } else {
                        logger.info("reload is not required")
                    }
                }
            }

            logger.info("resources have been updated")
        }, 15L, 15L, TimeUnit.SECONDS)
    }

    override fun doStop() {
        schedule?.cancel(false)
    }

    override fun doClose() { }

}