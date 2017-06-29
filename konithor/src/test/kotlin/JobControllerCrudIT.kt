package org.korhan.monithor


import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.korhan.konithor.IntegrationTestConfig
import org.korhan.konithor.IntegrationTestUtils
import org.korhan.konithor.persistence.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.function.Consumer

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = arrayOf(IntegrationTestConfig::class))
class JobControllerCrudIT {

    @Autowired
    lateinit var restUtils: IntegrationTestUtils

    @Autowired
    lateinit var repo: JobRepository

    @Before
    fun setup() {
    }

    @After
    fun tearDown() {
        repo.deleteAll()
    }

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val jobs = restUtils.createJobs("job1", "job2", "job3")
        assert.that(jobs.size, equalTo(3))
        for (job in jobs) {
            assert.that(restUtils.load(job.id), equalTo(job))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {
        val jobs = restUtils.createJobs("job1", "job2", "job3")
        for (job in jobs) {
            job.name = (job.name + "_1")
            restUtils.update(job)
        }
        for (job in jobs) {
            assert.that(restUtils.load(job.id).name, equalTo(job.name))
        }
    }

    @Test
    fun testDelete() {
        val jobs = restUtils.createJobs("job1", "job2", "job3")
        jobs.forEach(Consumer { job -> restUtils.delete(job) })
        for (job in jobs) {
            // assertNull(restUtils.load(job.id))
        }
    }

}