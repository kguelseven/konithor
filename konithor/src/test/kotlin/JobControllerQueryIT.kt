package org.korhan.monithor


import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
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
class JobControllerQueryIT {

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
        job2 = restUtils.createJobWithTags("foo2", true, "tag1", "tag20")
        job3 = restUtils.createJobWithTags("foo3", true, "tag40", "foo1")
    }

    @After
    fun tearDown() {
        repo.deleteAll()
    }

    /*
    @Test
    public void testPagingSorting() throws Exception {
        assertThat(getAll("name", 0, 1, null)).isEqualTo(Arrays.asList(job1));
        assertThat(getAll("name", 1, 1, null)).isEqualTo(Arrays.asList(job2));
        assertThat(getAll("name", 2, 1, null)).isEqualTo(Arrays.asList(job3));
        assertThat(getAll("name", 3, 1, null)).isEmpty();
    }
    */

    @Test
    fun testQueryTagOrName() {
        assert.that(restUtils.queryTagOrName("foo1"), equalTo(listOf(job1, job3)))
        assert.that(restUtils.queryTagOrName("foo"), equalTo(listOf(job1, job2, job3)))
        assert.that(restUtils.queryTagOrName("koo"), isEmpty)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByName() {
        assert.that(restUtils.getByName("oo"), equalTo(listOf(job1, job2, job3)))
        assert.that(restUtils.getByName("foo2"), equalTo(listOf(job2)))
        assert.that(restUtils.getByName("xoo"), isEmpty)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByTag() {
        assert.that(restUtils.getByTag("tag1"), equalTo(listOf(job1, job2)))
        assert.that(restUtils.getByTag("tag2"), equalTo(listOf(job1)))
        assert.that(restUtils.getByTag("tag40"), equalTo(listOf(job3)))
    }
}