package dev.fernandotorres.catalogo;

import dev.fernandotorres.catalogo.api.CursorPageResponse;
import dev.fernandotorres.catalogo.api.JobDetails;
import dev.fernandotorres.catalogo.api.JobListItem;
import dev.fernandotorres.catalogo.api.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "catalog.admin.api-key=integration-test-key")
class JobCatalogIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void configureServices(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
        properties.add("spring.data.redis.host", redis::getHost);
        properties.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void resetDatabaseAndCache() {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE job_postings RESTART IDENTITY CASCADE");
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível preparar o banco de teste", exception);
        }
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__seed_demo_jobs.sql"))
                .execute(dataSource);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void combinesSearchFiltersAndReturnsTheMatchingJob() {
        PageResponse<JobListItem> page = search("q=APIs&state=BA&workMode=REMOTE&contractType=CLT"
                + "&technology=Spring+Boot&salaryFrom=5000&salaryTo=9000");

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(JobListItem::id).containsExactly(1L);
    }

    @Test
    void returnsAnEmptyPageWhenFiltersDoNotMatch() {
        PageResponse<JobListItem> page = search("city=Salvador&contractType=PJ");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
    }

    @Test
    void sortsByNumericIdAndByTitle() {
        PageResponse<JobListItem> byId = search("sortBy=ID&direction=DESC&size=3");
        PageResponse<JobListItem> byTitle = search("sortBy=TITLE&direction=ASC&size=10");

        assertThat(byId.content()).extracting(JobListItem::id).containsExactly(10L, 9L, 8L);
        assertThat(byTitle.content()).extracting(JobListItem::title)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void offsetPagesDoNotRepeatItemsWithDeterministicIdOrdering() {
        PageResponse<JobListItem> first = search("sortBy=ID&direction=ASC&page=0&size=5");
        PageResponse<JobListItem> second = search("sortBy=ID&direction=ASC&page=1&size=5");

        assertThat(first.content()).extracting(JobListItem::id).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(second.content()).extracting(JobListItem::id).containsExactly(6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void cursorPagesTraverseTheCatalogWithoutRepeatingIds() {
        List<Long> ids = new ArrayList<>();
        String cursor = null;
        boolean hasNext;
        do {
            String query = "size=4" + (cursor == null ? "" : "&cursor=" + cursor);
            CursorPageResponse<JobListItem> window = cursorSearch(query);
            ids.addAll(window.content().stream().map(JobListItem::id).toList());
            cursor = window.nextCursor();
            hasNext = window.hasNext();
        } while (hasNext);

        assertThat(ids).hasSize(10);
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void rejectsInvalidCursorAndInvertedSalaryRange() {
        ResponseEntity<String> cursor = rest.getForEntity("/api/v1/jobs/scroll?cursor=invalid", String.class);
        ResponseEntity<String> salary = rest.getForEntity("/api/v1/jobs?salaryFrom=9000&salaryTo=5000", String.class);

        assertThat(cursor.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(salary.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnsDetailsWithSortedTechnologies() {
        ResponseEntity<JobDetails> response = rest.getForEntity("/api/v1/jobs/1", JobDetails.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().technologies()).containsExactly("Java", "PostgreSQL", "REST", "Spring Boot");
    }

    @Test
    void protectsAdminEndpointsWithAnApiKey() {
        ResponseEntity<String> response = rest.postForEntity("/api/v1/admin/jobs", new HttpEntity<>("{}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidatesCachedEmptySearchAfterPublishingAMatchingJob() {
        assertThat(search("q=cache").totalElements()).isZero();

        ResponseEntity<JobDetails> created = postAdmin(job("Cache Probe Engineer"));
        PageResponse<JobListItem> refreshed = search("q=cache");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(refreshed.totalElements()).isEqualTo(1);
        assertThat(refreshed.content().getFirst().title()).isEqualTo("Cache Probe Engineer");
    }

    @Test
    void invalidatesCachedDetailsAfterUpdatingAJob() {
        assertThat(rest.getForObject("/api/v1/jobs/1", JobDetails.class).title())
                .isEqualTo("Desenvolvedor Java Backend Pleno");

        HttpHeaders headers = adminHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(job("Título atualizado para teste"), headers);
        ResponseEntity<JobDetails> updated = rest.exchange("/api/v1/admin/jobs/1", HttpMethod.PUT,
                request, JobDetails.class);
        JobDetails detailsAfterUpdate = rest.getForObject("/api/v1/jobs/1", JobDetails.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsAfterUpdate.title()).isEqualTo("Título atualizado para teste");
    }

    @Test
    void invalidatesCachedSearchAfterDeactivatingAJob() {
        PageResponse<JobListItem> before = search("city=Salvador&state=BA&sortBy=ID&direction=ASC");
        assertThat(before.content()).extracting(JobListItem::id).contains(1L, 5L);

        HttpHeaders headers = adminHeaders();
        ResponseEntity<Void> deactivated = rest.exchange("/api/v1/admin/jobs/1/deactivate", HttpMethod.PATCH,
                new HttpEntity<>(headers), Void.class);
        PageResponse<JobListItem> after = search("city=Salvador&state=BA&sortBy=ID&direction=ASC");

        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(after.content()).extracting(JobListItem::id).containsExactly(5L);
    }

    @Test
    void servesTheDashboardAtTheRootAndKeepsOpenApiAvailable() {
        ResponseEntity<String> root = rest.getForEntity("/", String.class);
        ResponseEntity<String> dashboard = rest.getForEntity("/dashboard.html", String.class);
        ResponseEntity<String> openApi = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(root.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(root.getBody()).contains("Oportunidades", "dashboard.js");
        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody()).contains("Oportunidades", "dashboard.js");
        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApi.getBody()).contains("/api/v1/jobs");
    }

    private PageResponse<JobListItem> search(String query) {
        ResponseEntity<PageResponse<JobListItem>> response = rest.exchange("/api/v1/jobs?" + query,
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private CursorPageResponse<JobListItem> cursorSearch(String query) {
        ResponseEntity<CursorPageResponse<JobListItem>> response = rest.exchange("/api/v1/jobs/scroll?" + query,
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<JobDetails> postAdmin(Map<String, Object> body) {
        return rest.exchange("/api/v1/admin/jobs", HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders()), JobDetails.class);
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Admin-Key", "integration-test-key");
        return headers;
    }

    private Map<String, Object> job(String title) {
        return Map.ofEntries(
                Map.entry("title", title),
                Map.entry("company", "Equipe de Teste"),
                Map.entry("summary", "Cache Probe para testar invalidação."),
                Map.entry("description", "Vaga criada pela suíte de integração."),
                Map.entry("city", "Salvador"),
                Map.entry("state", "BA"),
                Map.entry("workMode", "REMOTE"),
                Map.entry("contractType", "CLT"),
                Map.entry("salaryMin", 5000),
                Map.entry("salaryMax", 7000),
                Map.entry("technologies", List.of("Java", "Spring Boot"))
        );
    }
}
