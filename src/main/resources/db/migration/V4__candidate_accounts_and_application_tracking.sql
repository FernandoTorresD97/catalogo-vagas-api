ALTER TABLE job_postings
    ADD COLUMN application_url VARCHAR(2048);

CREATE TABLE candidate_accounts (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_candidate_accounts_email UNIQUE (email)
);

CREATE TABLE job_apply_clicks (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_apply_clicks_job_time
    ON job_apply_clicks (job_posting_id, clicked_at DESC);

CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    candidate_account_id BIGINT NOT NULL REFERENCES candidate_accounts(id) ON DELETE CASCADE,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_application_candidate UNIQUE (job_posting_id, candidate_account_id)
);

CREATE INDEX idx_job_applications_job_reported
    ON job_applications (job_posting_id, reported_at DESC);
