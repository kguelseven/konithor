package org.korhan.monithor

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
class TagControllerIT {

    private var job1: Job? = null
    private var job2: Job? = null
    private var job3: Job? = null

    @Autowired
    lateinit var restUtils: IntegrationTestUtils

    @Autowired
    lateinit var repo: JobRepository

    @Before
    fun setup() {
        job1 = restUtils.createJobWithTags("foo1", true, "tag1", "tag2", "tag3")
        job2 = restUtils.createJobWithTags("foo2", false, "tag1", "tag20")
        job3 = restUtils.createJobWithTags("foo3", true, "tag40", "tag2")
    }

    @After
    fun tearDown() {
        repo.deleteAll()
    }

    @Test
    fun testGetTags() {
        assert.that(restUtils.getTags(), equalTo(listOf("TAG1", "TAG2", "TAG20", "TAG3", "TAG40")))
    }

    @Test
    fun testGetStatusSuccess() {
        val statusTag2 = restUtils.getTagStatus("TAG2")
        assert.that(statusTag2.success, equalTo(true))
        assert.that(statusTag2.jobs, equalTo(listOf(job1, job3)))
    }

    @Test
    fun testGetStatusFailure() {
        val statusTag1 = restUtils!!.getTagStatus("TAG1")
        assert.that(statusTag1.success, equalTo(false))
        assert.that(statusTag1.jobs, equalTo(listOf(job1, job2)))
        assert.that(statusTag1.jobs[0].lastResult, equalTo(true))
        assert.that(statusTag1.jobs[1].lastResult, equalTo(false))
    }
}