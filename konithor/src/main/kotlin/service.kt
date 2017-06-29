package org.korhan.konithor.service;

import org.korhan.konithor.check.Checker
import org.korhan.konithor.model.Job
import org.korhan.konithor.model.JobResult
import org.korhan.konithor.model.TagResult
import org.korhan.konithor.persistence.JobRepository
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors.toSet
import javax.validation.Valid

@RestController
@RequestMapping(value = "check")
class CheckController(private val repository: JobRepository, val checker: Checker) {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.POST))
    fun check(@RequestBody @Valid job: Job, bindingResult: BindingResult): Job {
        val jobResult: JobResult = checker.check(job)
        job.populateFromResult(jobResult)
        if (job.id != null) {
            return repository.save(job)
        }
        return job;
    }
}


@RestController
@RequestMapping(value = "jobs")
class JobController(private val repository: JobRepository) {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun queryJobs(@RequestParam("queryString") query: String): List<Job> {
        if (query.isNotBlank()) {
            return repository.findByTagOrName(query)
        } else {
            return repository.findAllByOrderByName()
        }
    }

    @RequestMapping(value = "/{id}", method = arrayOf(RequestMethod.GET))
    fun jobById(@PathVariable("id") id: Long): Job {
        return repository.findOne(id)
    }

    @RequestMapping(value = "/tag/{tag}", method = arrayOf(RequestMethod.GET))
    fun jobsByTag(@PathVariable("tag") tag: String): List<Job> {
        return repository.findByTag(tag.toUpperCase())
    }

    @RequestMapping(value = "/name/{name}", method = arrayOf(RequestMethod.GET))
    fun jobsByName(@PathVariable("name") name: String): List<Job> {
        return repository.findAllByNameIgnoreCaseContainingOrderByName(name)
    }

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.POST))
    fun save(@RequestBody @Valid job: Job, bindingResult: BindingResult): Job {
        return repository.save(uppercaseAllTags(job))
    }

    @RequestMapping(value = "/{id}", method = arrayOf(RequestMethod.PUT))
    fun update(@RequestBody @Valid job: Job, @PathVariable("id") id: Long, bindingResult: BindingResult): Job {
        return repository.save(uppercaseAllTags(job))
    }

    @RequestMapping(value = "/{id}", method = arrayOf(RequestMethod.DELETE))
    fun delete(@PathVariable("id") id: Long) {
        repository.delete(id)
    }

    private fun uppercaseAllTags(job: Job): Job {
        job.tags = job.tags.stream()
                .map(String::toUpperCase)
                .collect(toSet())
        return job;
    }
}


@RestController
@RequestMapping(value = "tags")
class TagController(private val repository: JobRepository) {

    @RequestMapping(value = "/", method = arrayOf(RequestMethod.GET))
    fun allTags(): List<String> = repository.findAllTags()

    @RequestMapping(value = "/status/{tag}", method = arrayOf(RequestMethod.GET))
    fun getStatus(@PathVariable("tag") tag: String): TagResult {
        val jobs = repository.findByTag(tag)
        val lastJob = jobs.maxBy { job -> job.lastBuildTimestamp }
        val successStatus = jobs.all { job -> job.lastResult }
        return TagResult(
                jobs = jobs,
                lastTimestamp = lastJob!!.lastTimestamp,
                tag = tag,
                success = successStatus)
    }
}