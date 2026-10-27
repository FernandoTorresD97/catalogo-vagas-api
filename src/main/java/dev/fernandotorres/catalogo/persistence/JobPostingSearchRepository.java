package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.JobPosting;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public interface JobPostingSearchRepository {

    List<JobPosting> searchWindow(
            Specification<JobPosting> filters,
            Instant cursorPublishedAt,
            Long cursorId,
            int limit
    );
}
