package dev.fernandotorres.catalogo.service;

import dev.fernandotorres.catalogo.api.JobDetails;
import dev.fernandotorres.catalogo.api.JobUpsertRequest;
import dev.fernandotorres.catalogo.cache.JobPostingChangedEvent;
import dev.fernandotorres.catalogo.cache.JobSearchSnapshot;
import dev.fernandotorres.catalogo.domain.JobPosting;
import dev.fernandotorres.catalogo.persistence.JobPostingRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobManagementService {

    private final JobPostingRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public JobManagementService(JobPostingRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobDetails create(JobUpsertRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        var job = new JobPosting(
                request.title(), request.company(), request.summary(), request.description(), request.city(),
                request.state(), request.workMode(), request.contractType(), request.salaryMin(), request.salaryMax(),
                request.publishedAt() == null ? Instant.now() : request.publishedAt(), request.technologies()
        );
        JobPosting saved = repository.save(job);
        eventPublisher.publishEvent(new JobPostingChangedEvent(saved.getId(), null, JobSearchSnapshot.from(saved)));
        return toDetails(saved);
    }

    @Transactional
    public JobDetails update(long id, JobUpsertRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        JobPosting job = findOrThrow(id);
        JobSearchSnapshot previous = JobSearchSnapshot.from(job);
        job.updateDetails(
                request.title(), request.company(), request.summary(), request.description(), request.city(),
                request.state(), request.workMode(), request.contractType(), request.salaryMin(), request.salaryMax(),
                request.publishedAt() == null ? job.getPublishedAt() : request.publishedAt(), request.technologies()
        );
        JobPosting saved = repository.save(job);
        eventPublisher.publishEvent(new JobPostingChangedEvent(saved.getId(), previous, JobSearchSnapshot.from(saved)));
        return toDetails(saved);
    }

    @Transactional
    public void deactivate(long id) {
        JobPosting job = findOrThrow(id);
        JobSearchSnapshot previous = JobSearchSnapshot.from(job);
        job.deactivate();
        eventPublisher.publishEvent(new JobPostingChangedEvent(job.getId(), previous, JobSearchSnapshot.from(job)));
    }

    private JobPosting findOrThrow(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vaga não encontrada"));
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "salaryMin não pode ser maior que salaryMax");
        }
    }

    private JobDetails toDetails(JobPosting job) {
        return new JobDetails(
                job.getId(), job.getTitle(), job.getCompany(), job.getSummary(), job.getDescription(),
                job.getCity(), job.getState(), job.getWorkMode(), job.getContractType(), job.getSalaryMin(),
                job.getSalaryMax(), job.getPublishedAt(), job.getTechnologies().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList()
        );
    }
}
