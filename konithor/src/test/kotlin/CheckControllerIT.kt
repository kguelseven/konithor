package org.korhan.konithor.service

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.korhan.konithor.IntegrationTestConfig
import org.korhan.konithor.IntegrationTestUtils
import org.korhan.konithor.model.Job
import org.korhan.konithor.persistence.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = arrayOf(IntegrationTestConfig::class))
class CheckControllerIT {

    @Autowired
    lateinit var restUtils: IntegrationTestUtils

    @Autowired
    lateinit var repository: JobRepository

    lateinit var job: Job

    @Before
    fun setup() {
        job = restUtils.createJobWithTags("dog fooding", true, "tag1", "tag2", "tag3")
    }

    @After
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun testCheckSuccess() {
        val job = newJob("fooding")
        val jobChecked = restUtils.check(job)
        assert.that(jobChecked.lastResult, equalTo(true))
    }

    @Test
    fun testCheckFailure() {
        val job = newJob("fLooding")
        val jobChecked = restUtils.check(job)
        assert.that(jobChecked.lastResult, equalTo(false))
    }

    @Test
    fun testCheckSuccessUpdate() {
        val job = newJob("fooding")
        var jobSaved = repository.save(job)
        val jobChecked = restUtils.check(jobSaved)
        jobSaved = repository.findOne(jobSaved.id)
        assert.that(jobSaved.lastResult, equalTo(true))
        assert.that(jobSaved, equalTo(jobChecked))
    }

    private fun newJob(successMatch: String): Job {
        return Job(
                name = "testing with " + successMatch,
                url = IntegrationTestUtils.JOBS_URL + job.id,
                successMatch = successMatch
        )
    }
}