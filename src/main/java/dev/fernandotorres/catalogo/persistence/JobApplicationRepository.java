package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    long countByJobPostingId(long jobId);
    boolean existsByJobPostingIdAndCandidateId(long jobId, long candidateId);

    @Modifying
    @Query(value = "INSERT INTO job_applications (job_posting_id, candidate_account_id, reported_at) " +
            "VALUES (:jobId, :candidateId, now()) ON CONFLICT (job_posting_id, candidate_account_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("jobId") long jobId, @Param("candidateId") long candidateId);
}
