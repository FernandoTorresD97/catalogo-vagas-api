package dev.fernandotorres.catalogo.cache;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.JobPosting;
import dev.fernandotorres.catalogo.domain.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobSearchSnapshot(
        String title,
        String company,
        String summary,
        String city,
        String state,
        WorkMode workMode,
        ContractType contractType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Instant publishedAt,
        boolean active,
        List<String> technologies
) {
    public static JobSearchSnapshot from(JobPosting job) {
        return new JobSearchSnapshot(
                job.getTitle(), job.getCompany(), job.getSummary(), job.getCity(), job.getState(),
                job.getWorkMode(), job.getContractType(), job.getSalaryMin(), job.getSalaryMax(),
                job.getPublishedAt(), job.isActive(), job.getTechnologies().stream().toList()
        );
    }
}
