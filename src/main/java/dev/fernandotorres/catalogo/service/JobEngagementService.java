package dev.fernandotorres.catalogo.service;

import dev.fernandotorres.catalogo.api.JobEngagementResponse;
import dev.fernandotorres.catalogo.domain.CandidateAccount;
import dev.fernandotorres.catalogo.domain.JobApplyClick;
import dev.fernandotorres.catalogo.domain.JobPosting;
import dev.fernandotorres.catalogo.persistence.CandidateAccountRepository;
import dev.fernandotorres.catalogo.persistence.JobApplicationRepository;
import dev.fernandotorres.catalogo.persistence.JobApplyClickRepository;
import dev.fernandotorres.catalogo.persistence.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobEngagementService {
    private final JobPostingRepository jobs;
    private final JobApplyClickRepository clicks;
    private final JobApplicationRepository applications;
    private final CandidateAccountRepository candidates;

    public JobEngagementService(JobPostingRepository jobs, JobApplyClickRepository clicks,
                                JobApplicationRepository applications, CandidateAccountRepository candidates) {
        this.jobs = jobs;
        this.clicks = clicks;
        this.applications = applications;
        this.candidates = candidates;
    }

    @Transactional
    public String recordClickAndGetUrl(long jobId) {
        JobPosting job = findActive(jobId);
        if (job.getApplicationUrl() == null) {
            throw new ResponseStatusException(CONFLICT, "Esta vaga ainda não tem link de candidatura");
        }
        clicks.save(new JobApplyClick(job));
        return job.getApplicationUrl();
    }

    @Transactional(readOnly = true)
    public JobEngagementResponse stats(long jobId, Long candidateId) {
        if (!jobs.existsById(jobId)) throw new ResponseStatusException(NOT_FOUND, "Vaga não encontrada");
        return new JobEngagementResponse(clicks.countByJobPostingId(jobId), applications.countByJobPostingId(jobId),
                candidateId != null && applications.existsByJobPostingIdAndCandidateId(jobId, candidateId));
    }

    @Transactional
    public JobEngagementResponse reportApplication(long jobId, String email) {
        JobPosting job = findActive(jobId);
        CandidateAccount candidate = candidates.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conta de candidato não encontrada"));
        applications.insertIfAbsent(jobId, candidate.getId());
        return stats(jobId, candidate.getId());
    }

    private JobPosting findActive(long id) {
        JobPosting job = jobs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vaga não encontrada"));
        if (!job.isActive()) throw new ResponseStatusException(NOT_FOUND, "Vaga não encontrada");
        return job;
    }
}
