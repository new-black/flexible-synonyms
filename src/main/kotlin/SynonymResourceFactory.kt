package io.newblack.elastic

import org.apache.http.impl.client.HttpClients
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.core.WhitespaceTokenizer

interface SynonymResourceFactory {
    fun create(resource: String, format: String): SynonymResource
}

class DefaultSynonymResourceFactory : SynonymResourceFactory {
    override fun create(resource: String, format: String): SynonymResource {
        // TODO(kevin): which analyzer should we pass to the synonym parser down the road?
        val analyzer = object : Analyzer() {
            override fun createComponents(fieldName: String?): TokenStreamComponents {
                val tokenizer = WhitespaceTokenizer()
                return TokenStreamComponents(tokenizer, LowerCaseFilter(tokenizer))
            }
        }

        return WebSynonymResource(true, analyzer, format, resource, HttpClients::createDefault)
    }

}