package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobDetails(
        Long id,
        String title,
        String company,
        String summary,
        String description,
        String city,
        String state,
        WorkMode workMode,
        ContractType contractType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Instant publishedAt,
        List<String> technologies,
        String applicationUrl
) {
}
