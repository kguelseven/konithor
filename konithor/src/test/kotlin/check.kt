package org.korhan.konithor.check


import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmptyString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.korhan.konithor.model.Job
import org.mockito.Matchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.runners.MockitoJUnitRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RunWith(MockitoJUnitRunner::class)
class DataExtractorTest {

    private val extractor: DataExtractor = DataExtractor()

    @Test
    fun testExtractVersionDefault() {
        assert.that(extractor.extractVersion("", "<p>1701.0.13</p>"), equalTo("1701.0.13"))
        assert.that(extractor.extractVersion("", " 1701.0.13 "), equalTo("1701.0.13"))
        assert.that(extractor.extractVersion("", " 1.0.0 "), equalTo("1.0.0"))
        assert.that(extractor.extractVersion("", "1212asd1<td>1.0.11-SNAPSHOT</td></tr><tr><td> applicationVersion=1701.0.14-SNAPSHOT"), equalTo("1.0.11-SNAPSHOT"))
        assert.that(extractor.extractVersion("", "<p> 1701.10.13-SNAPSHOT </p>abc"), equalTo("1701.10.13-SNAPSHOT"))
    }

    @Test
    fun testExtractVersionGivenPattern() {
        assert.that(extractor.extractVersion(".*(\\d{4}).*", "<p>1701</p>1 a"), equalTo("1701"))
        assert.that(extractor.extractVersion(".*Version = (\\d+).*", "<p>Version = 1701</p>1 a"), equalTo("1701"))
    }

    @Test
    fun testExtractBuildTimestampDefault() {
        assert.that(extractor.extractBuildTimestamp("", "<strong>build.timestamp</strong></td><td>2017-01-06 00:37:45</td>"), equalTo("2017-01-06 00:37:45"))
        assert.that(extractor.extractBuildTimestamp("", " 2017-01-06 00:37:45 2017-01-06 00:00:00 "), equalTo("2017-01-06 00:37:45"))
    }

    @Test
    fun testExtractBuildTimestampGivenPattern() {
        assert.that(extractor.extractBuildTimestamp(".*(\\d{4}).*", " 2017-01-06 00:37:45 "), equalTo("2017"))
    }
}


@RunWith(MockitoJUnitRunner::class)
class HttpCheckerTest {

    lateinit var testee: HttpChecker

    val extractor = DataExtractor()

    @Mock
    lateinit var client: HttpClient
    @Mock
    lateinit var httpEntity: HttpEntity
    @Mock
    lateinit var httpResponse: HttpResponse


    @Before
    @Throws(IOException::class)
    fun setup() {
        this.testee = HttpChecker(client, extractor)
        `when`(httpResponse.entity).thenReturn(httpEntity)
        `when`(httpEntity.content).thenReturn(ByteArrayInputStream("YfooU 12 x13 ver = 1.20<br> a".toByteArray(StandardCharsets.UTF_8)))
        `when`(client.execute(any(HttpGet::class.java))).thenReturn(httpResponse)
    }

    @Test
    fun testCheckSimpleString() {
        val result = testee.check(newJob("foo"))
        assert.that(result.success, equalTo(true))
        assert.that(result.error, isEmptyString)
    }

    @Test
    fun testCheckRegexString() {
        val result = testee.check(newJob("[a-z]{1}[o-o]{2}"))
        assert.that(result.success, equalTo(true))
        assert.that(result.error, isEmptyString)
    }

    @Test
    fun testCheckNonMatch() {
        val result = testee.check(newJob("/d{2}"))
        assert.that(result.success, equalTo(false))
        assert.that(result.error, equalTo("success match failed"))
    }

    @Test
    @Throws(IOException::class)
    fun testCheckIOException() {
        `when`<HttpResponse>(client.execute(any(HttpGet::class.java))).thenThrow(IOException("boom"))
        val result = testee.check(newJob("foo"))
        assert.that(result.success, equalTo(false))
        assert.that(result.error, containsSubstring("boom"))
    }

    @Test
    @Throws(IOException::class)
    fun testCheckDeploymentWrong() {
        val now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
        `when`<InputStream>(httpEntity.content).thenReturn(ByteArrayInputStream("1701.0.13-SNAPSHOT $now foo".toByteArray(StandardCharsets.UTF_8)))
        val result = testee.check(newJobCheckDeployment("foo"))
        assert.that(result.success, equalTo(true))
        assert.that(result.buildTimestamp, equalTo(now))
        assert.that(result.version, equalTo("1701.0.13-SNAPSHOT"))
        assert.that(result.error, isEmptyString)
    }

    @Test
    @Throws(IOException::class)
    fun testCheckDeploymentWrongBuildTimestamp() {
        `when`<InputStream>(httpEntity.content).thenReturn(ByteArrayInputStream("1701.0.13-SNAPSHOT 2017-01-06 00:37:45 foo".toByteArray(StandardCharsets.UTF_8)))
        val result = testee.check(newJobCheckDeployment("foo"))
        assert.that(result.success, equalTo(false))
        assert.that(result.buildTimestamp, equalTo("2017-01-06 00:37:45"))
        assert.that(result.version, equalTo("1701.0.13-SNAPSHOT"))
        assert.that(result.error, equalTo("deployment version check failed"))
    }

    @Test
    @Throws(IOException::class)
    fun testCheckDeploymentMissingVersion() {
        `when`<InputStream>(httpEntity.content).thenReturn(ByteArrayInputStream("2017-01-06 00:37:45 foo".toByteArray(StandardCharsets.UTF_8)))
        val result = testee.check(newJobCheckDeployment("foo"))
        assert.that(result.success, equalTo(false))
        assert.that(result.version, isEmptyString)
        assert.that(result.buildTimestamp, equalTo("2017-01-06 00:37:45"))
        assert.that(result.error, equalTo("deployment version check failed"))
    }

    @Test
    @Throws(IOException::class)
    fun testCheckDeploymentMissingBuildtimestamp() {
        `when`<InputStream>(httpEntity.content).thenReturn(ByteArrayInputStream("1701.0.13 foo".toByteArray(StandardCharsets.UTF_8)))
        val result = testee.check(newJobCheckDeployment("foo"))
        assert.that(result.success, equalTo(false))
        assert.that(result.buildTimestamp, isEmptyString)
        assert.that(result.version, equalTo("1701.0.13"))
        assert.that(result.error, equalTo("deployment version check failed"))
    }

    private fun newJob(successMatch: String): Job {
        return Job(
                name = "testing",
                successMatch = successMatch,
                url = "testing"
        )
    }

    private fun newJobCheckDeployment(successMatch: String): Job {
        val job = newJob(successMatch)
        job.checkDeployment = true
        return job
    }
}
