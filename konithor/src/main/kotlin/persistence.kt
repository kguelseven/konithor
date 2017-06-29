package org.korhan.konithor.persistence;

import org.korhan.konithor.model.Job
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JobRepository : JpaRepository<Job, Long> {

    fun findAllByOrderByName(): List<Job>

    fun findAllByNameIgnoreCaseContainingOrderByName(name: String): List<Job>

    @Query("SELECT j FROM Job j WHERE :tag MEMBER OF j.tags ORDER BY j.name")
    fun findByTag(@Param("tag") tag: String): List<Job>

    @Query("SELECT j FROM Job j WHERE UPPER(j.name) LIKE UPPER(CONCAT('%',:value,'%')) OR UPPER(:value) MEMBER OF j.tags ORDER BY j.name")
    fun findByTagOrName(@Param("value") value: String): List<Job>

    @Query(value = "SELECT DISTINCT(tag) FROM TAGS ORDER BY tag", nativeQuery = true)
    fun findAllTags(): List<String>

}
