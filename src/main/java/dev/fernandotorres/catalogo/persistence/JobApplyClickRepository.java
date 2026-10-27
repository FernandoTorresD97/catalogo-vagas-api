package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.JobApplyClick;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplyClickRepository extends JpaRepository<JobApplyClick, Long> {
    long countByJobPostingId(long jobId);
}
