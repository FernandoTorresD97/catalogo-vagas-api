package dev.fernandotorres.catalogo.api;

import dev.fernandotorres.catalogo.service.JobManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@Tag(name = "Job administration", description = "Publicação e manutenção de vagas")
@SecurityRequirement(name = "AdminApiKey")
public class JobAdminController {

    private final JobManagementService service;

    public JobAdminController(JobManagementService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Publica uma vaga")
    public ResponseEntity<JobDetails> create(@Valid @RequestBody JobUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma vaga")
    public JobDetails update(@PathVariable @Min(1) long id, @Valid @RequestBody JobUpsertRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa uma vaga sem removê-la do histórico")
    public ResponseEntity<Void> deactivate(@PathVariable @Min(1) long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
