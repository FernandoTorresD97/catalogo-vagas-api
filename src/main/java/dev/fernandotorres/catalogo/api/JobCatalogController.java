package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.WorkMode;
import dev.fernandotorres.catalogo.service.JobCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Job catalog", description = "Consulta e busca de vagas publicadas")
public class JobCatalogController {

    private final JobCatalogService service;

    public JobCatalogController(JobCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Busca vagas com filtros combináveis, ordenação e paginação")
    public PageResponse<JobListItem> search(
            @Parameter(description = "Busca parcial no título, empresa ou resumo")
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Size(max = 100) String city,
            @RequestParam(required = false) @Size(min = 2, max = 2) String state,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) @Size(max = 80) String technology,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal salaryFrom,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal salaryTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "PUBLISHED_AT") JobSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        return service.search(q, city, state, workMode, contractType, technology,
                salaryFrom, salaryTo, page, size, sortBy, direction);
    }

    @GetMapping("/scroll")
    @Operation(summary = "Busca a próxima janela de vagas com cursor baseado em data e ID")
    public CursorPageResponse<JobListItem> scroll(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Size(max = 100) String city,
            @RequestParam(required = false) @Size(min = 2, max = 2) String state,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) @Size(max = 80) String technology,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal salaryFrom,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal salaryTo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 200) String cursor
    ) {
        return service.scroll(q, city, state, workMode, contractType, technology,
                salaryFrom, salaryTo, size, cursor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os detalhes de uma vaga")
    public JobDetails findById(@PathVariable @Min(1) long id) {
        return service.findById(id);
    }
}
