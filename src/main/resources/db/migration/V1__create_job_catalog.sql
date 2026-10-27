CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    company VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    work_mode VARCHAR(20) NOT NULL,
    contract_type VARCHAR(20) NOT NULL,
    salary_min NUMERIC(12, 2),
    salary_max NUMERIC(12, 2),
    published_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_job_salary_range CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max
    )
);

CREATE TABLE job_posting_technologies (
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    technology VARCHAR(80) NOT NULL,
    PRIMARY KEY (job_posting_id, technology)
);

CREATE INDEX idx_jobs_active_published_id
    ON job_postings (active, published_at DESC, id DESC);

CREATE INDEX idx_jobs_active_state_mode_published_id
    ON job_postings (active, state, work_mode, published_at DESC, id DESC);

CREATE INDEX idx_job_technologies_technology_job
    ON job_posting_technologies (lower(technology), job_posting_id);
