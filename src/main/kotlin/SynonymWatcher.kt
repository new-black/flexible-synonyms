package io.newblack.elastic

import org.elasticsearch.common.component.AbstractLifecycleComponent
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.Index
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

interface SynonymWatcher {
    fun startWatching(index: Index, filter: DynamicSynonymFilter, resource: SynonymResource)
    fun stopWatching(index: Index)
}

data class FilterWithResource(val filter: DynamicSynonymFilter, val resource: SynonymResource)

class ScheduledSynonymWatcher(
        settings: Settings,
        private val scheduler: ScheduledExecutorService
) : AbstractLifecycleComponent(settings), SynonymWatcher {

    private val filters: MutableMap<String, MutableCollection<FilterWithResource>> = ConcurrentHashMap()

    private var schedule: ScheduledFuture<*>? = null

    override fun startWatching(index: Index, filter: DynamicSynonymFilter, resource: SynonymResource) {
        logger.info("start watching filter/resource for index {}", index.name)
        filters.getOrPut(index.uuid, ::mutableListOf).add(FilterWithResource(filter, resource))
    }

    override fun stopWatching(index: Index) {
        logger.info("stop watching all filters/resources for index {}", index.name)
        filters.remove(index.uuid)
    }

    override fun doStart() {
        schedule = scheduler.scheduleAtFixedRate({
            logger.debug("updating resources of {} filters..", filters.count())

            filters.forEach { it ->
                it.value.forEach {
                    val (filter, resource) = it

                    logger.debug("checking if reload is required..")
                    if (resource.needsReload()) {
                        logger.debug("reload is required")
                        filter.update(resource.load())
                    } else {
                        logger.debug("reload is not required")
                    }
                }
            }

            logger.debug("resources have been updated")
        }, 15L, 15L, TimeUnit.SECONDS)
    }

    override fun doStop() {
        schedule?.cancel(false)
    }

    override fun doClose() {}

}