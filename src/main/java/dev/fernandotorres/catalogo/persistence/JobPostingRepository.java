package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobPostingRepository extends
        JpaRepository<JobPosting, Long>,
        JpaSpecificationExecutor<JobPosting>,
        JobPostingSearchRepository {
}
