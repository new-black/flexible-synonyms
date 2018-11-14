package io.newblack.elastic

import org.elasticsearch.client.Client
import org.elasticsearch.cluster.service.ClusterService
import org.elasticsearch.common.component.AbstractLifecycleComponent
import org.elasticsearch.common.logging.ESLoggerFactory
import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.index.Index
import org.elasticsearch.index.IndexModule
import org.elasticsearch.index.IndexService
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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class FlexibleSynonymsPlugin : Plugin(), AnalysisPlugin {

    companion object {
        const val LOGGER_NAME = "flexible-synonyms"
    }

    val logger = ESLoggerFactory.getLogger(LOGGER_NAME)

    private lateinit var watcher: FlexibleSynonymWatcher

    init {
        logger.info("plugin constructed")
    }

    override fun onIndexModule(indexModule: IndexModule) {
        indexModule.addIndexEventListener(object : IndexEventListener {
            override fun afterIndexCreated(indexService: IndexService) {
                logger.info(
                        "after index created called {}",
                        indexService.index().name
                )
            }

            override fun afterIndexRemoved(index: Index?, indexSettings: IndexSettings?, reason: IndicesClusterStateService.AllocatedIndices.IndexRemovalReason?) {
                logger.info("after index removed called {} because {}", index?.name, reason?.name)
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

        // TODO(kevin): stop watcher if index is removed
        // TODO(kevin): probably have to keep a map with index > watcher
        watcher = FlexibleSynonymWatcher(clusterService.settings, threadPool.scheduler())

        return mutableListOf(watcher)
    }

    override fun getTokenFilters(): MutableMap<String, AnalysisProvider<TokenFilterFactory>> {
        logger.info("getTokenFilters invoked")
        return mutableMapOf(
                "flexible_synonym" to AnalysisProvider<TokenFilterFactory> {
                    indexSettings, environment, name, settings -> FlexibleSynonymTokenFilterFactory(indexSettings, name, settings, watcher)
                }
        )
    }

    override fun getSettings(): MutableList<Setting<*>> {
        return mutableListOf(SYNONYM_PATH)
    }
}

class FlexibleSynonymWatcher(
        settings: Settings,
        private val scheduler: ScheduledExecutorService
) : AbstractLifecycleComponent(settings) {

    private val filters: MutableCollection<Pair<DynamicSynonymFilter, SynonymResource>> = mutableListOf()

    private var schedule: ScheduledFuture<*>? = null

    init {
        logger.info("FlexibleSynonymWatcher initialized with settings: {}", settings)
    }

    fun watch(filter: DynamicSynonymFilter, resource: SynonymResource) {
        filters.add(Pair(filter, resource))
    }

    override fun doStart() {
        logger.info("doStart() called for FlexibleSynonymWatcher")

        schedule = scheduler.scheduleAtFixedRate({
            logger.info("updating resources..")
            filters.forEach {
                val m = it.second.load()

                logger.info("loaded synonyms: {}, {}", m, m.fst)

                it.first.update(m)
            }
            logger.info("resources have been updated")
        }, 30L, 30L, TimeUnit.SECONDS)
    }

    override fun doStop() {
        logger.info("doStop() called for FlexibleSynonymWatcher")

        schedule?.cancel(false)
    }

    override fun doClose() {
        logger.info("doClose() called for FlexibleSynonymWatcher")
    }

}