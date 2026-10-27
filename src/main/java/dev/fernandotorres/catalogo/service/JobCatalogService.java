package dev.fernandotorres.catalogo.service;

import dev.fernandotorres.catalogo.api.JobDetails;
import dev.fernandotorres.catalogo.api.CursorPageResponse;
import dev.fernandotorres.catalogo.api.JobListItem;
import dev.fernandotorres.catalogo.api.JobSortField;
import dev.fernandotorres.catalogo.api.PageResponse;
import dev.fernandotorres.catalogo.cache.JobRedisCache;
import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.JobPosting;
import dev.fernandotorres.catalogo.domain.WorkMode;
import dev.fernandotorres.catalogo.persistence.JobPostingRepository;
import dev.fernandotorres.catalogo.persistence.JobSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobCatalogService {

    private final JobPostingRepository repository;
    private final JobRedisCache cache;

    public JobCatalogService(JobPostingRepository repository, JobRedisCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobListItem> search(
            String queryText,
            String city,
            String state,
            WorkMode workMode,
            ContractType contractType,
            String technology,
            BigDecimal salaryFrom,
            BigDecimal salaryTo,
            int page,
            int size,
            JobSortField sortBy,
            Sort.Direction direction
    ) {
        validateSalaryRange(salaryFrom, salaryTo);
        var cacheKey = cache.searchKey("offset", queryText, city, state, workMode, contractType, technology,
                salaryFrom, salaryTo, page, size, sortBy.name(), direction.name(), null);
        var cached = cache.getPage(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        var sort = Sort.by(direction, sortBy.property()).and(Sort.by(direction, "id"));
        var pageable = PageRequest.of(page, size, sort);
        var specification = JobSpecifications.search(
                queryText, city, state, workMode, contractType, technology, salaryFrom, salaryTo
        );
        var response = PageResponse.from(repository.findAll(specification, pageable).map(this::toListItem));
        cache.putPage(cacheKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<JobListItem> scroll(
            String queryText,
            String city,
            String state,
            WorkMode workMode,
            ContractType contractType,
            String technology,
            BigDecimal salaryFrom,
            BigDecimal salaryTo,
            int size,
            String cursor
    ) {
        validateSalaryRange(salaryFrom, salaryTo);
        var cacheKey = cache.searchKey("cursor", queryText, city, state, workMode, contractType, technology,
                salaryFrom, salaryTo, 0, size, "PUBLISHED_AT", "DESC", cursor);
        var cached = cache.getCursorPage(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        CursorPosition position = decodeCursor(cursor);
        var specification = JobSpecifications.search(
                queryText, city, state, workMode, contractType, technology, salaryFrom, salaryTo
        );
        List<JobPosting> fetched = repository.searchWindow(
                specification,
                position == null ? null : position.publishedAt(),
                position == null ? null : position.id(),
                size + 1
        );

        boolean hasNext = fetched.size() > size;
        List<JobPosting> window = fetched.subList(0, Math.min(size, fetched.size()));
        String nextCursor = hasNext && !window.isEmpty()
                ? encodeCursor(window.getLast())
                : null;

        var response = new CursorPageResponse<>(window.stream().map(this::toListItem).toList(), size, hasNext, nextCursor);
        cache.putCursorPage(cacheKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public JobDetails findById(long id) {
        var cached = cache.getDetails(id);
        if (cached.isPresent()) {
            return cached.get();
        }
        JobDetails details = repository.findById(id)
                .map(this::toDetails)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vaga não encontrada"));
        cache.putDetails(id, details);
        return details;
    }

    private JobListItem toListItem(JobPosting job) {
        return new JobListItem(
                job.getId(), job.getTitle(), job.getCompany(), job.getSummary(), job.getCity(), job.getState(),
                job.getWorkMode(), job.getContractType(), job.getSalaryMin(), job.getSalaryMax(), job.getPublishedAt()
        );
    }

    private JobDetails toDetails(JobPosting job) {
        return new JobDetails(
                job.getId(), job.getTitle(), job.getCompany(), job.getSummary(), job.getDescription(),
                job.getCity(), job.getState(), job.getWorkMode(), job.getContractType(), job.getSalaryMin(),
                job.getSalaryMax(), job.getPublishedAt(), sortedTechnologies(job), job.getApplicationUrl()
        );
    }

    private List<String> sortedTechnologies(JobPosting job) {
        return job.getTechnologies().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private String encodeCursor(JobPosting lastItem) {
        String value = lastItem.getPublishedAt() + "|" + lastItem.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private CursorPosition decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor malformado");
            }
            Instant publishedAt = Instant.parse(parts[0]);
            long id = Long.parseLong(parts[1]);
            if (id < 1) {
                throw new IllegalArgumentException("ID de cursor inválido");
            }
            return new CursorPosition(publishedAt, id);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Cursor inválido", exception);
        }
    }

    private void validateSalaryRange(BigDecimal salaryFrom, BigDecimal salaryTo) {
        if (salaryFrom != null && salaryTo != null && salaryFrom.compareTo(salaryTo) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "salaryFrom não pode ser maior que salaryTo");
        }
    }

    private record CursorPosition(Instant publishedAt, long id) {
    }
}
