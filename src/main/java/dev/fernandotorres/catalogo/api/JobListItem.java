package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;

public record JobListItem(
        Long id,
        String title,
        String company,
        String summary,
        String city,
        String state,
        WorkMode workMode,
        ContractType contractType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Instant publishedAt
) {
}
