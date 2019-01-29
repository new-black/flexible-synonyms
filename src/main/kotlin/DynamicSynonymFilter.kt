package io.newblack.elastic

import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.synonym.SynonymGraphFilter
import org.apache.lucene.analysis.synonym.SynonymMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class DynamicSynonymFilter(input: TokenStream, synonyms: SynonymMap) : TokenFilter(input) {

    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    private lateinit var filter: TokenFilter

    init {
        update(synonyms)
    }

    fun update(synonyms: SynonymMap) {
        lock.writeLock().lock()

        filter = if (synonyms.fst == null) {
            NoopTokenFilter(input)
        } else {
            SynonymGraphFilter(input, synonyms, true)
        }

        lock.writeLock().unlock()
    }

    override fun incrementToken(): Boolean {
        return filter.incrementToken()
    }

    override fun reset() {
        filter.reset()
    }

    override fun close() {
        filter.close()
    }

    override fun end() {
        filter.end()
    }
}

class NoopTokenFilter(input: TokenStream) : TokenFilter(input) {
    override fun incrementToken() = false
}