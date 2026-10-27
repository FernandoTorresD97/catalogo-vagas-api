# Catálogo de Vagas API

API REST em Java para consultar e administrar vagas publicadas, com filtros combináveis, duas formas de paginação e cache Redis.

## Stack

- Java 21
- Spring Boot 3.5.16
- Spring Web, Validation e Data JPA
- PostgreSQL 17, Redis 7 e Flyway
- Spring Boot Actuator para métricas HTTP
- Swagger UI / OpenAPI

## Executar

Pré-requisitos: Java 21 e Docker Desktop. O Maven Wrapper baixa a versão necessária na primeira execução.

Na pasta do projeto:

```powershell
docker compose up -d postgres redis
$env:ADMIN_API_KEY = "local-dev-admin-key"
.\mvnw.cmd spring-boot:run
```

O dashboard do catálogo fica em `http://localhost:8080` e a documentação da API em `http://localhost:8080/swagger-ui.html`. O dashboard oferece busca por cargo/empresa, filtros de contrato, modalidade e localidade, ordenação, paginação e detalhe da vaga. O Flyway cria o esquema e insere vagas fictícias na primeira inicialização. Os serviços locais usam credenciais de desenvolvimento e as portas ficam limitadas ao computador local. A chave mostrada é apenas para desenvolvimento; escolha outra para qualquer ambiente compartilhado.
Para gerar o JAR executável:

```powershell
.\mvnw.cmd -DskipTests package
java -jar .\target\catalogo-vagas-api-0.1.0-SNAPSHOT.jar
```

## Endpoints

- `GET /api/v1/jobs` — lista vagas ativas com filtros opcionais.
- `GET /api/v1/jobs/scroll` — percorre vagas por cursor, sem calcular a contagem total.
- `GET /api/v1/jobs/{id}` — retorna os detalhes de uma vaga.
- `POST /api/v1/admin/jobs` — publica uma vaga (requer `X-Admin-Key`).
- `PUT /api/v1/admin/jobs/{id}` — atualiza uma vaga (requer `X-Admin-Key`).
- `PATCH /api/v1/admin/jobs/{id}/deactivate` — desativa uma vaga (requer `X-Admin-Key`).
- `GET /swagger-ui.html` — interface da documentação OpenAPI.
- `GET /v3/api-docs` — contrato OpenAPI em JSON.
- `GET /actuator/metrics/http.server.requests` — métricas de duração e volume das requisições HTTP.

## Testes automatizados

Execute `.\mvnw.cmd test` com o Docker Desktop ativo. A suíte usa PostgreSQL e Redis efêmeros via Testcontainers e recria os dados fictícios entre os casos. Ela cobre filtros combinados e vazios, ordenação, paginação por deslocamento e cursor, validações, detalhes, contrato OpenAPI, autenticação administrativa e invalidação de cache após publicar, atualizar ou desativar uma vaga.

As rotas administrativas usam `X-Admin-Key`. Configure `ADMIN_API_KEY` antes de iniciar a aplicação; sem essa variável, a API administrativa responde `503`.

Exemplo para publicar uma vaga no PowerShell:

```powershell
$vaga = @{
  title = "Desenvolvedor Java Backend"
  company = "Empresa de demonstração"
  summary = "APIs com Java, Spring Boot e PostgreSQL."
  description = "Atuação em serviços backend e consultas SQL."
  city = "Salvador"
  state = "BA"
  workMode = "REMOTE"
  contractType = "CLT"
  salaryMin = 5000
  salaryMax = 7000
  technologies = @("Java", "Spring Boot", "PostgreSQL")
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/admin/jobs" `
  -Headers @{ "X-Admin-Key" = $env:ADMIN_API_KEY } `
  -ContentType "application/json" -Body $vaga
```

Exemplo com filtros combinados:

```text
GET /api/v1/jobs?state=BA&workMode=REMOTE&technology=Java&salaryFrom=5000&page=0&size=10&sortBy=PUBLISHED_AT&direction=DESC
```

No PowerShell, consulte a primeira página com:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/jobs?state=BA&workMode=REMOTE&technology=Java&salaryFrom=5000&page=0&size=10"
```

Exemplo usando cursor:

```text
GET /api/v1/jobs/scroll?state=BA&workMode=REMOTE&technology=Java&salaryFrom=5000&size=10
```

Use o valor de `nextCursor` da resposta na próxima chamada, preservando os mesmos filtros:

```text
GET /api/v1/jobs/scroll?state=BA&workMode=REMOTE&technology=Java&salaryFrom=5000&size=10&cursor=<nextCursor>
```

Filtros disponíveis: `q`, `city`, `state`, `workMode`, `contractType`, `technology`, `salaryFrom` e `salaryTo`.

Ordenação disponível: `PUBLISHED_AT`, `TITLE`, `COMPANY` ou `SALARY_MIN`. O padrão é `PUBLISHED_AT DESC`. Cada ordenação recebe também `id` como desempate, para manter a ordem determinística entre requisições. O tamanho da página vai de 1 a 100.

Na paginação por cursor, a ordem é fixa em `publishedAt DESC, id DESC`. Envie o `nextCursor` recebido como parâmetro `cursor` para buscar a próxima janela. O cursor é opaco para o cliente e não expõe o cálculo do banco. Essa rota não calcula `totalElements` ou `totalPages`, evitando a consulta de contagem. Alterações nos dados entre duas requisições ainda podem mover uma vaga que tenha sua data de publicação alterada; o cursor evita o custo de deslocamentos longos, mas não cria uma fotografia imutável do catálogo.

O filtro `salaryFrom` considera vagas cujo teto salarial informado alcance o valor solicitado; `salaryTo` considera vagas cujo piso informado não ultrapasse esse valor. Vagas sem salário informado não entram quando um filtro salarial é aplicado.

## Decisões desta primeira versão

- A consulta usa `Specification` para combinar filtros sem criar um método de repositório para cada combinação.
- A busca por tecnologia usa uma subconsulta `EXISTS`, evitando duplicar vagas no resultado por causa da tabela de tecnologias.
- A ordenação usa uma lista permitida de campos; nomes de propriedades recebidos do cliente não são passados diretamente ao JPA.
- A migração inclui índices para o fluxo padrão e para consultas por estado/modalidade. Eles são um ponto de partida; devem ser comparados com `EXPLAIN ANALYZE` em uma base maior antes de serem tratados como definitivos.
- A busca textual usa `LIKE` com padrão parcial; índices GIN com `pg_trgm` foram adicionados para título, empresa e resumo. O PostgreSQL documenta suporte trigram para consultas `LIKE`; padrões curtos podem aproveitar menos o índice. [Documentação pg_trgm](https://www.postgresql.org/docs/17/pgtrgm.html)
- Paginação por deslocamento é simples para páginas iniciais, mas uma inserção entre requisições pode deslocar itens. A rota por cursor é a alternativa para navegação profunda.
- As respostas de listagem são cacheadas por hash dos filtros, ordenação e página/cursor, com TTL de 60 segundos. Detalhes são cacheados pela chave `catalog:jobs:detail:{id}` por 10 minutos.
- `JobPostingChangedEvent` invalida a chave de detalhe, as respostas de busca que continham a vaga e as consultas cujos filtros correspondiam aos dados antigos ou novos. O índice de consultas é mantido no Redis; não é necessário limpar o banco de cache inteiro.
- As rotas administrativas publicam o evento na mesma transação que grava a mudança; o listener executa a invalidação depois do commit. O TTL de 60 segundos limita a janela de uma corrida entre uma leitura em andamento e a invalidação.
- Se Redis estiver indisponível, a leitura continua pelo PostgreSQL; o cache é ignorado e o erro é registrado no log.

## Análise de desempenho

Para aumentar a base local com 25 mil vagas fictícias, execute uma vez:

```powershell
Get-Content .\scripts\load-performance-data.sql | docker exec -i catalogo-vagas-postgres psql -U vagas -d vagas_catalogo
```

Compare filtros e páginas usando `/actuator/metrics/http.server.requests`. Para analisar o lado do banco, execute a consulta correspondente com `EXPLAIN (ANALYZE, BUFFERS)` no PostgreSQL e registre o plano antes e depois de alterar índices. As métricas do Actuator são para desenvolvimento; proteja-as se publicar a aplicação.

## Próximos incrementos

1. Registrar medições de latência e planos de consulta antes/depois dos índices.
2. Testes de integração com PostgreSQL e Redis reais para filtros, resultado vazio, paginação, cache e invalidação.

As vagas de demonstração são fictícias. Nenhum dado pessoal ou vaga real é usado pelo projeto.


