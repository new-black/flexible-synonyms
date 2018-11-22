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
        lock.readLock().lock()
        val success = filter.incrementToken()
        lock.readLock().unlock()
        return success
    }

    override fun reset() {
        lock.readLock().lock()
        filter.reset()
        lock.readLock().unlock()
    }

    override fun close() {
        lock.readLock().lock()
        filter.close()
        lock.readLock().unlock()
    }

    override fun end() {
        lock.readLock().lock()
        filter.end()
        lock.readLock().unlock()
    }
}

class NoopTokenFilter(input: TokenStream) : TokenFilter(input) {
    override fun incrementToken() = false
}