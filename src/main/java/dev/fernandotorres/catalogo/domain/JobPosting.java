package dev.fernandotorres.catalogo.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 160)
    private String company;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    private ContractType contractType;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "application_url", length = 2048)
    private String applicationUrl;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_posting_technologies", joinColumns = @JoinColumn(name = "job_posting_id"))
    @Column(name = "technology", nullable = false, length = 80)
    private Set<String> technologies = new HashSet<>();

    protected JobPosting() {
    }

    public JobPosting(
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
            Set<String> technologies,
            String applicationUrl
    ) {
        updateDetails(title, company, summary, description, city, state, workMode, contractType,
                salaryMin, salaryMax, publishedAt, technologies, applicationUrl);
        this.active = true;
    }

    public void updateDetails(
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
            Set<String> technologies,
            String applicationUrl
    ) {
        this.title = title.trim();
        this.company = company.trim();
        this.summary = summary.trim();
        this.description = description.trim();
        this.city = city.trim();
        this.state = state.trim().toUpperCase(Locale.ROOT);
        this.workMode = workMode;
        this.contractType = contractType;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.applicationUrl = applicationUrl == null || applicationUrl.isBlank() ? null : applicationUrl.trim();
        this.publishedAt = publishedAt;
        this.technologies.clear();
        technologies.stream().map(String::trim).filter(value -> !value.isBlank()).forEach(this.technologies::add);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public boolean isActive() {
        return active;
    }

    public Set<String> getTechnologies() {
        return technologies;
    }
}
