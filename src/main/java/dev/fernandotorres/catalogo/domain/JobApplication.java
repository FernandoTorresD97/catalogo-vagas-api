package dev.fernandotorres.catalogo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_applications", uniqueConstraints = @UniqueConstraint(
        name = "uq_job_application_candidate", columnNames = {"job_posting_id", "candidate_account_id"}))
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_account_id", nullable = false)
    private CandidateAccount candidate;
    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt = Instant.now();
    protected JobApplication() { }
    public JobApplication(JobPosting jobPosting, CandidateAccount candidate) {
        this.jobPosting = jobPosting;
        this.candidate = candidate;
    }
}
