package org.korhan.konithor.runner


import org.korhan.konithor.check.Checker
import org.korhan.konithor.globals.loggerFor
import org.korhan.konithor.model.Job
import org.korhan.konithor.persistence.JobRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.*

@Component
class JobRunner(private val repository: JobRepository, executor: Executor, private val checker: Checker) {

    private val log = loggerFor(javaClass)
    private val completionService: CompletionService<Job> = ExecutorCompletionService(executor)

    @Scheduled(fixedDelayString = "\${runner.delay}")
    @Throws(Exception::class)
    fun run() {
        val startMillis = System.currentTimeMillis()
        val jobs = loadDueJobs()
        logJobsLoaded(jobs)
        startJobs(jobs)
        val jobsChecked = waitForResults(jobs.size)
        setNotCheckedResult(jobs, jobsChecked)
        updateJobs(jobs)
        logJobsFinished(jobs, startMillis)
    }

    private fun updateJobs(jobs: List<Job>) = jobs.forEach { job -> repository.save(job) }

    private fun startJobs(jobs: List<Job>) = jobs.forEach { job ->
        completionService.submit(JobTask(checker, job))
    }

    private fun loadDueJobs() = repository.findAll().filter { job -> job.isDue() }


    @Throws(InterruptedException::class)
    private fun waitForResults(jobsCount: Int): Set<Job> {
        val results = HashSet<Job>()
        for (i in 0..jobsCount - 1) {
            try {
                val future = completionService.take()
                results.add(future.get())
            } catch (ee: ExecutionException) {
                log.error("ExecutionException executing task", ee.cause)
            } catch (ex: Exception) {
                log.error("Error executing task", ex)
            }

        }
        return results
    }

    private fun setNotCheckedResult(allJobs: List<Job>, jobsChecked: Set<Job>) {
        val jobsNotChecked = HashSet(allJobs)
        jobsNotChecked.removeAll(jobsChecked)
        for (job in jobsNotChecked) {
            job.lastResult = false
            job.lastTimestamp = System.currentTimeMillis()
            job.lastMessage = "timeout?"
            job.lastDuration = 0L
            job.lastVersion = ""
            job.lastBuildTimestamp = ""
        }
    }

    private fun logJobsFinished(jobs: List<Job>, startMillis: Long) {
        if (!jobs.isEmpty()) {
            log.debug("${jobs.size} jobs run in ${(System.currentTimeMillis() - startMillis)} ms")
        }
    }

    private fun logJobsLoaded(jobs: List<Job>) {
        if (!jobs.isEmpty()) {
            log.debug("Total jobs loaded ${jobs.size}")
        }
    }

    internal inner class JobTask(private val checker: Checker, private val job: Job) : Callable<Job> {

        @Throws(Exception::class)
        override fun call(): Job {
            job.populateFromResult(checker.check(job))
            return job
        }
    }
}
