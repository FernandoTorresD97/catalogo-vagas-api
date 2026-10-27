-- Run once against the local catalog database to add 25,000 synthetic vacancies.
BEGIN;

CREATE TEMP TABLE generated_job_ids (id BIGINT PRIMARY KEY) ON COMMIT DROP;

WITH inserted_jobs AS (
    INSERT INTO job_postings
        (title, company, summary, description, city, state, work_mode, contract_type,
         salary_min, salary_max, published_at, active)
    SELECT
        'Vaga de carga #' || n,
        'Empresa de demonstração ' || (n % 700),
        'Vaga Java e Spring Boot para conjunto de dados de desempenho.',
        'Descrição sintética para medir buscas combinadas e paginação no catálogo.',
        (ARRAY['Salvador', 'Feira de Santana', 'São Paulo', 'Recife', 'Fortaleza', 'Cruz das Almas'])[1 + (n % 6)],
        (ARRAY['BA', 'BA', 'SP', 'PE', 'CE', 'BA'])[1 + (n % 6)],
        (ARRAY['REMOTE', 'HYBRID', 'ON_SITE'])[1 + (n % 3)],
        (ARRAY['CLT', 'PJ', 'OTHER'])[1 + (n % 3)],
        4000 + (n % 8000),
        7000 + (n % 8000),
        now() - (n || ' minutes')::interval,
        TRUE
    FROM generate_series(1, 25000) AS n
    RETURNING id
)
INSERT INTO generated_job_ids (id)
SELECT id FROM inserted_jobs;

INSERT INTO job_posting_technologies (job_posting_id, technology)
SELECT id, 'Java' FROM generated_job_ids
UNION ALL
SELECT id, 'Spring Boot' FROM generated_job_ids WHERE mod(id, 2) = 0
UNION ALL
SELECT id, 'PostgreSQL' FROM generated_job_ids WHERE mod(id, 3) = 0
UNION ALL
SELECT id, 'Redis' FROM generated_job_ids WHERE mod(id, 5) = 0;

COMMIT;
