INSERT INTO job_postings
    (title, company, summary, description, city, state, work_mode, contract_type, salary_min, salary_max, published_at, active)
VALUES
    ('Desenvolvedor Java Backend Pleno', 'Acme Tecnologia', 'Desenvolvimento de APIs Java com Spring Boot e PostgreSQL.', 'Atuação em APIs REST, consultas SQL e manutenção de serviços backend.', 'Salvador', 'BA', 'REMOTE', 'CLT', 6500, 8500, now() - interval '1 day', TRUE),
    ('Desenvolvedor Java Júnior', 'Norte Sistemas', 'Construção e manutenção de serviços Spring Boot.', 'Trabalho com controllers, services, repositórios e integração com PostgreSQL.', 'Feira de Santana', 'BA', 'HYBRID', 'CLT', 3800, 5200, now() - interval '2 days', TRUE),
    ('Backend Engineer Java', 'Nuvem Digital', 'APIs de consulta com alto volume de leitura.', 'Implementação de endpoints REST, paginação e análise de consultas no PostgreSQL.', 'São Paulo', 'SP', 'REMOTE', 'PJ', 8000, 11000, now() - interval '3 days', TRUE),
    ('Pessoa Desenvolvedora Java', 'Farol Tech', 'Sistemas internos e integração de serviços.', 'Desenvolvimento Java, Spring Data JPA, SQL e documentação OpenAPI.', 'Recife', 'PE', 'HYBRID', 'CLT', 6000, 7800, now() - interval '4 days', TRUE),
    ('Estágio em Desenvolvimento Backend', 'Ponto de Partida', 'Apoio em APIs Java e rotinas de banco de dados.', 'Acompanhamento de equipe backend com Java, Git e SQL.', 'Salvador', 'BA', 'ON_SITE', 'INTERN', 1500, 2200, now() - interval '5 days', TRUE),
    ('Java Software Developer', 'Atlas Serviços', 'Manutenção de aplicações Java e APIs REST.', 'Atuação em serviços Java, testes automatizados e integração com PostgreSQL.', 'Belo Horizonte', 'MG', 'REMOTE', 'PJ', 9000, 12500, now() - interval '6 days', TRUE),
    ('Desenvolvedor Backend', 'Cacto Labs', 'Serviços Java com foco em qualidade e desempenho.', 'Implementação de funcionalidades com Spring Boot, JPA e SQL.', 'Fortaleza', 'CE', 'HYBRID', 'CLT', 5500, 7500, now() - interval '7 days', TRUE),
    ('Analista Desenvolvedor Java', 'Litoral Tech', 'Evolução de sistemas e integrações REST/SOAP.', 'Manutenção de sistemas Java e apoio a consultas em banco relacional.', 'Cruz das Almas', 'BA', 'REMOTE', 'CLT', 5000, 6800, now() - interval '8 days', TRUE),
    ('Desenvolvedor Java Sênior', 'Ponte Digital', 'APIs de catálogo com PostgreSQL e Redis.', 'Responsabilidade por consultas, cache de leitura e monitoramento de latência.', 'São Paulo', 'SP', 'REMOTE', 'PJ', 12000, 16000, now() - interval '9 days', TRUE),
    ('Backend Java Developer', 'Vereda Sistemas', 'Desenvolvimento de APIs para plataforma SaaS.', 'Trabalho com Java, Spring Boot, testes e integração contínua.', 'Aracaju', 'SE', 'ON_SITE', 'CLT', 4800, 6500, now() - interval '10 days', TRUE);

INSERT INTO job_posting_technologies (job_posting_id, technology)
SELECT job.id, skill.technology_name
FROM job_postings job
JOIN (VALUES
    ('Desenvolvedor Java Backend Pleno', 'Java'),
    ('Desenvolvedor Java Backend Pleno', 'Spring Boot'),
    ('Desenvolvedor Java Backend Pleno', 'PostgreSQL'),
    ('Desenvolvedor Java Backend Pleno', 'REST'),
    ('Desenvolvedor Java Júnior', 'Java'),
    ('Desenvolvedor Java Júnior', 'Spring Boot'),
    ('Desenvolvedor Java Júnior', 'SQL'),
    ('Desenvolvedor Java Júnior', 'Git'),
    ('Backend Engineer Java', 'Java'),
    ('Backend Engineer Java', 'PostgreSQL'),
    ('Backend Engineer Java', 'Redis'),
    ('Backend Engineer Java', 'OpenAPI'),
    ('Pessoa Desenvolvedora Java', 'Java'),
    ('Pessoa Desenvolvedora Java', 'Spring Data JPA'),
    ('Pessoa Desenvolvedora Java', 'SQL'),
    ('Pessoa Desenvolvedora Java', 'OpenAPI'),
    ('Estágio em Desenvolvimento Backend', 'Java'),
    ('Estágio em Desenvolvimento Backend', 'Git'),
    ('Estágio em Desenvolvimento Backend', 'SQL'),
    ('Java Software Developer', 'Java'),
    ('Java Software Developer', 'PostgreSQL'),
    ('Java Software Developer', 'JUnit'),
    ('Desenvolvedor Backend', 'Java'),
    ('Desenvolvedor Backend', 'Spring Boot'),
    ('Desenvolvedor Backend', 'JPA'),
    ('Desenvolvedor Backend', 'SQL'),
    ('Analista Desenvolvedor Java', 'Java'),
    ('Analista Desenvolvedor Java', 'Spring Boot'),
    ('Analista Desenvolvedor Java', 'REST'),
    ('Analista Desenvolvedor Java', 'SOAP'),
    ('Desenvolvedor Java Sênior', 'Java'),
    ('Desenvolvedor Java Sênior', 'PostgreSQL'),
    ('Desenvolvedor Java Sênior', 'Redis'),
    ('Desenvolvedor Java Sênior', 'SQL'),
    ('Backend Java Developer', 'Java'),
    ('Backend Java Developer', 'Spring Boot'),
    ('Backend Java Developer', 'Git'),
    ('Backend Java Developer', 'JUnit')
) AS skill(job_title, technology_name) ON skill.job_title = job.title;
