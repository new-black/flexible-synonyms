package io.newblack.elastic.test

import io.newblack.elastic.WebSynonymResource
import org.apache.http.HttpHeaders
import org.apache.http.ProtocolVersion
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebSynonymResourceTests {

    private val client = mock(CloseableHttpClient::class.java)
    private val response = mock(CloseableHttpResponse::class.java)
    private val resource  = WebSynonymResource(true, StandardAnalyzer(), "", "lala.txt") {
        client
    }

    @Before
    fun before() {
        `when`(client.execute(any())).thenReturn(response)
    }

    @Test
    fun needsReloadShouldReturnTrue() {
        `when`(response.statusLine).thenReturn(BasicStatusLine(ProtocolVersion("http", 2, 1), 200, "OK"))

        assertTrue { resource.needsReload() }
    }

    @Test
    fun needsReloadShouldReturnFalse() {
        `when`(response.statusLine).thenReturn(BasicStatusLine(ProtocolVersion("http", 2, 1), 304, "OK"))

        assertFalse { resource.needsReload() }
    }

    @Test
    fun needsReloadShouldSendModifiedAndETagHeaders() {
        val expectedLastModified = LocalDateTime.now().toString()
        val expectedETag = "abcdef"

        `when`(response.statusLine).thenReturn(BasicStatusLine(ProtocolVersion("http", 2, 1), 200, "OK"))

        `when`(response.getLastHeader(HttpHeaders.LAST_MODIFIED))
                .thenReturn(BasicHeader(HttpHeaders.LAST_MODIFIED, expectedLastModified))

        `when`(response.getLastHeader(HttpHeaders.ETAG))
                .thenReturn(BasicHeader(HttpHeaders.ETAG, expectedETag))


        // Allow resource to set headers from response
        assertTrue { resource.needsReload() }

        val captor = ArgumentCaptor.forClass(HttpUriRequest::class.java)

        // Call it again, should send last modified and etag headers
        resource.needsReload()

        verify(client, times(2)).execute(captor.capture())

        assertEquals(expectedLastModified, captor.value.getLastHeader(HttpHeaders.IF_MODIFIED_SINCE)?.value)
        assertEquals(expectedETag, captor.value.getLastHeader(HttpHeaders.IF_NONE_MATCH)?.value)
    }

    @Test
    fun shouldLoadSynonyms() {
        `when`(response.statusLine).thenReturn(BasicStatusLine(ProtocolVersion("http", 2, 1), 200, "OK"))

        `when`(response.entity).thenReturn(
                BasicHttpEntity().apply {
                    val bytes = "med,medicine,medical".toByteArray()

                    contentLength = bytes.size.toLong()
                    content = bytes.inputStream()
                }
        )

        val map = resource.load()

        assertNotNull(map.fst)
        assertEquals(3, map.words.size())
    }

    private fun <T> any(): T {
        return ArgumentMatchers.any<T>()
    }

}