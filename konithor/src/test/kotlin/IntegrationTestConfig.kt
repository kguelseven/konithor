package org.korhan.konithor

import org.korhan.konithor.model.Job
import org.korhan.konithor.model.TagResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriComponentsBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter


@ComponentScan(basePackages = arrayOf("org.korhan.konithor.service", "org.korhan.konithor.check", "org.korhan.konithor.persistence"))
@EnableAutoConfiguration
class IntegrationTestConfig {
    // separate Config here since we don't want Scheduled-Jobs to run during tests

    @Bean
    fun restUtils() = IntegrationTestUtils()

    @Bean
    fun httpClient() = Konithor().httpClient()

    @Bean
    fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val jsonConverter = MappingJackson2HttpMessageConverter()
        val objectMapper = ObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.registerModule(KotlinModule())
        jsonConverter.objectMapper = objectMapper
        return jsonConverter
    }
}


class IntegrationTestUtils {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    val parameterizedTypeRefListJob: ParameterizedTypeReference<List<Job>> = object : ParameterizedTypeReference<List<Job>>() {}
    val parameterizedTypeRefListString: ParameterizedTypeReference<List<String>> = object : ParameterizedTypeReference<List<String>>() {}

    internal fun createJobWithTags(name: String, result: Boolean, vararg tags: String): Job {
        val job = Job(
                intervalSecs = 1,
                url = "http://korhan.org",
                successMatch = "testSuccess",
                lastResult = result,
                name = name
        )
        job.tags.addAll(tags)
        return restTemplate.postForObject(JOBS_URL, job, Job::class.java)
    }

    internal fun createJobs(vararg names: String): List<Job> {
        val jobs = mutableListOf<Job>()
        names.forEach { name -> jobs.add(createJobWithTags(name, true)) }
        return jobs
    }

    internal fun queryTagOrName(queryString: String): List<Job> {
        val builder = UriComponentsBuilder.fromHttpUrl(JOBS_URL).queryParam("queryString", queryString)
        val uri = builder.build().encode().toUri()
        return restTemplate.exchange<List<Job>>(uri, HttpMethod.GET, null, parameterizedTypeRefListJob).body
    }

    internal fun getByTag(tag: String): List<Job> {
        return restTemplate.exchange<List<Job>>(JOBS_URL + "tag/{tag}", HttpMethod.GET, null,
                parameterizedTypeRefListJob, tag).body
    }

    internal fun getByName(name: String): List<Job> {
        return restTemplate.exchange<List<Job>>(JOBS_URL + "name/{name}", HttpMethod.GET, null,
                parameterizedTypeRefListJob, name).body
    }

    internal fun getTags(): List<String> = restTemplate.exchange(TAGS_URL, HttpMethod.GET, null,
            parameterizedTypeRefListString).body

    internal fun getTagStatus(tag: String): TagResult {
        return restTemplate.getForObject(TAGS_URL + "status/{tag}", TagResult::class.java, tag)
    }

    internal fun check(job: Job) = restTemplate.postForObject(CHECK_URL, job, Job::class.java)

    internal fun delete(job: Job) = restTemplate.delete(JOBS_URL + "{id}", job.id)

    internal fun update(job: Job) = restTemplate.put(JOBS_URL + "{id}", job, job.id)

    internal fun load(id: Long?) = restTemplate.getForObject(JOBS_URL + "{id}", Job::class.java, id!!)

    companion object {
        internal val JOBS_URL = "http://localhost:8888/jobs/"
        internal val TAGS_URL = "http://localhost:8888/tags/"
        internal val CHECK_URL = "http://localhost:8888/check/"
    }
}
