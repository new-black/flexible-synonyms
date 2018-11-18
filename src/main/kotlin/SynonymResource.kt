package io.newblack.elastic

import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.synonym.SolrSynonymParser
import org.apache.lucene.analysis.synonym.SynonymMap
import org.apache.lucene.analysis.synonym.WordnetSynonymParser
import org.elasticsearch.common.logging.ESLoggerFactory
import java.net.URL

interface SynonymResource {
    fun load(): SynonymMap
    fun needsReload(): Boolean
}

class WebSynonymResource(
        private val expand: Boolean,
        private val analyzer: Analyzer,
        private val format: String,
        private val location: String,
        private val createClient: () -> CloseableHttpClient
) : SynonymResource {

    private val logger = ESLoggerFactory.getLogger(WebSynonymResource::class.java)

    private var lastModified: String? = null
    private var eTags: String? = null

    override fun load(): SynonymMap {
        val parser = createParser(format, expand, analyzer)

        logger.debug("loading from remote location {}", location)

        parser.parse(URL(location).openStream().reader())

        logger.debug("loaded from remote location")

        return parser.build()
    }

    override fun needsReload(): Boolean {
        logger.debug("checking if reload is required for: {}", location)

        val request = HttpHead(location)
                .apply {
                    lastModified?.let {
                        setHeader(HttpHeaders.IF_MODIFIED_SINCE, it)
                    }
                    eTags?.let {
                        setHeader(HttpHeaders.IF_NONE_MATCH, it)
                    }
                }

        var reloadRequired = false

        createClient().use { client ->
            client.execute(request).use {
                logger.debug("response status for HEAD: {}", it.statusLine)

                if (it.statusLine.statusCode == HttpStatus.SC_OK) {
                    // Update last modified and etag for next request
                    lastModified = it.getLastHeader(HttpHeaders.LAST_MODIFIED)?.value
                    eTags = it.getLastHeader(HttpHeaders.ETAG)?.value

                    logger.debug("updating last modified with: {}", lastModified)
                    logger.debug("updating etag with: {}", eTags)

                    reloadRequired = true
                }
            }
        }

        return reloadRequired
    }

}

fun createParser(format: String, expand: Boolean, analyzer: Analyzer): SynonymMap.Parser {
    if ("wordnet".equals(format, true)) {
        return WordnetSynonymParser(true, expand, analyzer)
    }

    return SolrSynonymParser(true, expand, analyzer)
}