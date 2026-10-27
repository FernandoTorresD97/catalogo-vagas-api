package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.JobPosting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JobPostingSearchRepositoryImpl implements JobPostingSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<JobPosting> searchWindow(
            Specification<JobPosting> filters,
            Instant cursorPublishedAt,
            Long cursorId,
            int limit
    ) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(JobPosting.class);
        var root = query.from(JobPosting.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(filters.toPredicate(root, query, criteriaBuilder));

        if (cursorPublishedAt != null && cursorId != null) {
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.lessThan(root.get("publishedAt"), cursorPublishedAt),
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("publishedAt"), cursorPublishedAt),
                            criteriaBuilder.lessThan(root.get("id"), cursorId)
                    )
            ));
        }

        query.select(root)
                .where(criteriaBuilder.and(predicates.toArray(Predicate[]::new)))
                .orderBy(criteriaBuilder.desc(root.get("publishedAt")), criteriaBuilder.desc(root.get("id")));

        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }
}
