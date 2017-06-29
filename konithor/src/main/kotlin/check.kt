package org.korhan.konithor.check;

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.korhan.konithor.model.Job
import org.korhan.konithor.model.JobResult
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

interface Checker {

    fun check(job: Job): JobResult

}

@Component
class HttpChecker(private val client: HttpClient, private val extractor: DataExtractor) : Checker {

    override fun check(job: Job): JobResult {
        val startMs = System.currentTimeMillis()
        var version: String = ""
        var buildTimestamp: String = ""
        var error: String = ""
        try {
            val response = httpCall(job)
            val text = readText(response)
            version = extractor.extractVersion(job.versionMatch, text)
            buildTimestamp = extractor.extractBuildTimestamp(job.buildTimestampMatch, text)
            if (!containsSuccessMatch(job.successMatch, text)) {
                error = "success match failed"
            } else if (job.checkDeployment) {
                if (!hasExpectedDeployment(version, buildTimestamp)) {
                    error = "deployment version check failed"
                }
            }
        } catch (ex: Exception) {
            // log.error("Error running check", ex)
            error = getError(ex)
        }

        return JobResult(
                job = job,
                buildTimestamp = buildTimestamp,
                version = version,
                success = (error.isBlank()),
                error = error,
                duration = (System.currentTimeMillis() - startMs))
    }

    private fun containsSuccessMatch(successMatch: String, text: String) = Pattern.matches(".*$successMatch.*", text)

    private fun hasExpectedDeployment(version: String?, buildTimestamp: String?): Boolean {
        if (buildTimestamp != null && version != null && version.contains("SNAPSHOT")) {
            val buildDate = LocalDate.parse(buildTimestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            return LocalDate.now().isEqual(buildDate)
        }
        return false
    }

    @Throws(IOException::class)
    private fun readText(response: HttpResponse): String {
        val text = EntityUtils.toString(response.entity, "UTF-8")
        return text.replace("\\r\\n|\\r|\\n".toRegex(), " ")
    }

    @Throws(IOException::class)
    private fun httpCall(job: Job) = client.execute(HttpGet(job.url))

    private fun getError(ex: Exception): String {
        var error = ex.javaClass.simpleName
        if (ex.message != null) {
            error += ": " + ex.message
        }
        return error
    }
}


@Component
class DataExtractor {

    fun extractVersion(patternOverwrite: String, text: String) = extractVersion(getPattern(VERSION_PATTERN, patternOverwrite), text)

    fun extractBuildTimestamp(patternOverwrite: String, text: String) = extractVersion(getPattern(BUILD_TIMESTAMP_PATTERN, patternOverwrite), text)

    private fun getPattern(pattern: Pattern, patternOverwrite: String) = if (patternOverwrite.isNotBlank()) Pattern.compile(patternOverwrite) else pattern

    private fun extractVersion(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        if (matcher.matches()) {
            return matcher.group(1)
        }
        return ""
    }

    companion object {
        private val VERSION_PATTERN = Pattern.compile(".*?(\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?).*")
        private val BUILD_TIMESTAMP_PATTERN = Pattern.compile(".*?(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}).*")
    }
}
