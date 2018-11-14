package io.newblack.elastic

import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.synonym.SynonymGraphFilter
import org.apache.lucene.analysis.synonym.SynonymMap

class DynamicSynonymFilter(
        input: TokenStream,
        synonyms: SynonymMap
) : TokenFilter(input) {

    private var filter: SynonymGraphFilter = SynonymGraphFilter(input, synonyms, true)

    fun update(synonyms: SynonymMap) {
        // TODO(kevin): make atomic
        filter = SynonymGraphFilter(input, synonyms, true)
    }

    override fun incrementToken(): Boolean = filter.incrementToken()

    override fun reset() = filter.reset()

    override fun close() = filter.close()

    override fun end() = filter.end()
}