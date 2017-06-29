package org.korhan.konithor.model;

import org.hibernate.validator.constraints.URL
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
data class Job(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long? = null,
        @NotNull
        var name: String? = null,
        @NotNull @URL
        var url: String,
        @NotNull
        var successMatch: String,
        @NotNull
        var intervalSecs: Int = 300,
        var versionMatch: String = "",
        var buildTimestampMatch: String = "",
        var checkDeployment: Boolean = false,
        var lastTimestamp: Long = 0L,
        var lastResult: Boolean = false,
        var lastMessage: String = "",
        var lastVersion: String = "",
        var lastBuildTimestamp: String = "",
        var lastDuration: Long = 0L,

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "TAGS", joinColumns = arrayOf(JoinColumn(name = "job_id")))
        @Column(name = "tag")
        var tags: MutableSet<String> = HashSet()) {

    // Default constructor for JPA
    private constructor() : this(url = "", successMatch = "")

    fun populateFromResult(result: JobResult) {
        lastResult = result.success
        lastTimestamp = System.currentTimeMillis()
        lastMessage = result.error
        lastDuration = result.duration
        lastVersion = result.version
        lastBuildTimestamp = result.buildTimestamp
    }

    fun isDue() = (System.currentTimeMillis() - lastTimestamp) > (1000 * intervalSecs)

}

class JobResult(
        val job: Job,
        val success: Boolean,
        val duration: Long,
        val version: String = "",
        val buildTimestamp: String = "",
        val error: String = ""
)

class TagResult(
        val tag: String,
        val success: Boolean,
        val lastTimestamp: Long,
        val jobs: List<Job>
)