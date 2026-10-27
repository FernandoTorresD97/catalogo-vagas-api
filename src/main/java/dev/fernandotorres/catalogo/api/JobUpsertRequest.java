package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.WorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record JobUpsertRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 160) String company,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank String description,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotNull WorkMode workMode,
        @NotNull ContractType contractType,
        @DecimalMin("0.00") BigDecimal salaryMin,
        @DecimalMin("0.00") BigDecimal salaryMax,
        Instant publishedAt,
        @NotNull @Size(min = 1, max = 20) Set<@NotBlank @Size(max = 80) String> technologies
) {
}
