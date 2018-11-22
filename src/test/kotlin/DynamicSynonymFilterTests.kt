package io.newblack.elastic.test

import io.newblack.elastic.DynamicSynonymFilter
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.synonym.SynonymMap
import org.junit.Test
import java.io.StringReader
import kotlin.test.fail

class DynamicSynonymFilterTests {

    @Test
    fun shouldNotThrowWhenSynonymsMapIsEmpty() {
        val tokenizer = WhitespaceTokenizer()
        tokenizer.setReader(StringReader("abc"))

        try {
            val filter = DynamicSynonymFilter(tokenizer, SynonymMap.Builder().build())
            filter.incrementToken()
        } catch (e: Throwable) {
            fail("Did not expect incrementToken to throw")
        }
    }

}