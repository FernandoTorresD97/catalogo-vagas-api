package dev.fernandotorres.catalogo.persistence;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.JobPosting;
import dev.fernandotorres.catalogo.domain.WorkMode;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<JobPosting> search(
            String queryText,
            String city,
            String state,
            WorkMode workMode,
            ContractType contractType,
            String technology,
            BigDecimal salaryFrom,
            BigDecimal salaryTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (hasText(queryText)) {
                String pattern = contains(queryText);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("company")), pattern, '\\'),
                        cb.like(cb.lower(root.get("summary")), pattern, '\\')
                ));
            }
            if (hasText(city)) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(state)) {
                predicates.add(cb.equal(cb.upper(root.get("state")), state.trim().toUpperCase(Locale.ROOT)));
            }
            if (workMode != null) {
                predicates.add(cb.equal(root.get("workMode"), workMode));
            }
            if (contractType != null) {
                predicates.add(cb.equal(root.get("contractType"), contractType));
            }
            if (hasText(technology)) {
                var technologyQuery = query.subquery(Integer.class);
                var job = technologyQuery.from(JobPosting.class);
                Join<JobPosting, String> technologies = job.join("technologies");
                technologyQuery.select(cb.literal(1));
                technologyQuery.where(
                        cb.equal(job.get("id"), root.get("id")),
                        cb.equal(cb.lower(technologies), technology.trim().toLowerCase(Locale.ROOT))
                );
                predicates.add(cb.exists(technologyQuery));
            }
            if (salaryFrom != null) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("salaryMax")),
                        cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryFrom)
                ));
            }
            if (salaryTo != null) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("salaryMin")),
                        cb.lessThanOrEqualTo(root.get("salaryMin"), salaryTo)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
