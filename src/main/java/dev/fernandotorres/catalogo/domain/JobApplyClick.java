package dev.fernandotorres.catalogo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_apply_clicks")
public class JobApplyClick {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;
    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt = Instant.now();
    protected JobApplyClick() { }
    public JobApplyClick(JobPosting jobPosting) { this.jobPosting = jobPosting; }
}
