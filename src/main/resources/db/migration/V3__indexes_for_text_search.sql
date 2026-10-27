CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_jobs_title_trgm
    ON job_postings USING GIN (lower(title) gin_trgm_ops);

CREATE INDEX idx_jobs_company_trgm
    ON job_postings USING GIN (lower(company) gin_trgm_ops);

CREATE INDEX idx_jobs_summary_trgm
    ON job_postings USING GIN (lower(summary) gin_trgm_ops);

CREATE INDEX idx_jobs_active_city_published_id
    ON job_postings (active, lower(city), published_at DESC, id DESC);
