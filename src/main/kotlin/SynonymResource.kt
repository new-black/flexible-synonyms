package io.newblack.elastic

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.synonym.SolrSynonymParser
import org.apache.lucene.analysis.synonym.SynonymMap
import org.apache.lucene.analysis.synonym.WordnetSynonymParser
import org.apache.lucene.util.CharsRef
import org.elasticsearch.common.logging.ESLoggerFactory
import java.net.URL

interface SynonymResource {
    fun load(): SynonymMap
}

class LocalSynonymResource(
        private val expand: Boolean,
        private val analyzer: Analyzer,
        private val format: String,
        private val location: String
) : SynonymResource {

    val logger = ESLoggerFactory.getLogger(LocalSynonymResource::class.java)

    override fun load(): SynonymMap {
        // TODO(kevin): implement this
//        logger.info("loading file from {}", location)
//
//        val parser = createParser(format, expand, analyzer)
//        parser.parse(File(ClassLoader.getSystemResource(location).file).reader())
//
//        logger.info("parsed file {}", location)
//
//        return parser.build()

        return SynonymMap.Builder().apply {
            add(CharsRef("med"), CharsRef("medicine"), true)
            add(CharsRef("med"), CharsRef("medical"), true)
        }.build()
    }

}

class WebSynonymResource(
        private val expand: Boolean,
        private val analyzer: Analyzer,
        private val format: String,
        private val location: String
) : SynonymResource {

    val logger = ESLoggerFactory.getLogger(WebSynonymResource::class.java)

    override fun load(): SynonymMap {
        logger.info("loading from remote location: {}", location)

        val parser = createParser(format, expand, analyzer)
        parser.parse(URL(location).openStream().reader())

        logger.info("loaded from remote location")

        return parser.build()
    }

}

fun createParser(format: String, expand: Boolean, analyzer: Analyzer): SynonymMap.Parser {
    if ("wordnet".equals(format, true)) {
        return WordnetSynonymParser(true, expand, analyzer)
    }

    return SolrSynonymParser(true, expand, analyzer)
}