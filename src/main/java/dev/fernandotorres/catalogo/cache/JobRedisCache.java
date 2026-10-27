package dev.fernandotorres.catalogo.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fernandotorres.catalogo.api.CursorPageResponse;
import dev.fernandotorres.catalogo.api.JobDetails;
import dev.fernandotorres.catalogo.api.JobListItem;
import dev.fernandotorres.catalogo.api.PageResponse;
import dev.fernandotorres.catalogo.domain.ContractType;
import dev.fernandotorres.catalogo.domain.WorkMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class JobRedisCache {

    private static final Logger log = LoggerFactory.getLogger(JobRedisCache.class);
    private static final Duration SEARCH_TTL = Duration.ofSeconds(60);
    private static final Duration SEARCH_INDEX_TTL = Duration.ofMinutes(5);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final String SEARCH_PREFIX = "catalog:jobs:search:";
    private static final String SEARCH_META_PREFIX = "catalog:jobs:search-meta:";
    private static final String SEARCH_INDEX = "catalog:jobs:search-index";
    private static final String DETAIL_PREFIX = "catalog:jobs:detail:";
    private static final String RELATED_SEARCH_PREFIX = "catalog:jobs:searches-by-job:";

    private static final TypeReference<PageResponse<JobListItem>> PAGE_TYPE = new TypeReference<>() {};
    private static final TypeReference<CursorPageResponse<JobListItem>> CURSOR_TYPE = new TypeReference<>() {};
    private static final TypeReference<SearchKey> SEARCH_KEY_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JobRedisCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public SearchCacheKey searchKey(
            String paginationMode,
            String queryText,
            String city,
            String state,
            WorkMode workMode,
            ContractType contractType,
            String technology,
            BigDecimal salaryFrom,
            BigDecimal salaryTo,
            int page,
            int size,
            String sortBy,
            String direction,
            String cursor
    ) {
        var query = new SearchKey(
                paginationMode, normalize(queryText), normalize(city), normalizeState(state), workMode, contractType,
                normalize(technology), decimal(salaryFrom), decimal(salaryTo), page, size, sortBy, direction, cursor
        );
        try {
            String redisKey = SEARCH_PREFIX + sha256(objectMapper.writeValueAsString(query));
            return new SearchCacheKey(redisKey, query);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível gerar a chave da busca", exception);
        }
    }

    public Optional<PageResponse<JobListItem>> getPage(SearchCacheKey key) {
        return read(key.redisKey(), PAGE_TYPE);
    }

    public Optional<CursorPageResponse<JobListItem>> getCursorPage(SearchCacheKey key) {
        return read(key.redisKey(), CURSOR_TYPE);
    }

    public void putPage(SearchCacheKey key, PageResponse<JobListItem> value) {
        writeSearch(key, value, value.content().stream().map(JobListItem::id).toList());
    }

    public void putCursorPage(SearchCacheKey key, CursorPageResponse<JobListItem> value) {
        writeSearch(key, value, value.content().stream().map(JobListItem::id).toList());
    }

    public Optional<JobDetails> getDetails(long id) {
        return read(DETAIL_PREFIX + id, new TypeReference<JobDetails>() {});
    }

    public void putDetails(long id, JobDetails details) {
        write(DETAIL_PREFIX + id, details, DETAIL_TTL);
    }

    public void evictJob(JobPostingChangedEvent event) {
        long id = event.jobId();
        try {
            String relatedKey = RELATED_SEARCH_PREFIX + id;
            Set<String> affectedSearches = new HashSet<>();
            Set<String> searchesContainingJob = redis.opsForSet().members(relatedKey);
            if (searchesContainingJob != null) {
                affectedSearches.addAll(searchesContainingJob);
            }

            long now = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore(SEARCH_INDEX, 0, now);
            Set<String> activeSearches = redis.opsForZSet().rangeByScore(SEARCH_INDEX, now + 1, Double.MAX_VALUE);
            if (activeSearches != null) {
                for (String searchKey : activeSearches) {
                    String metadata = redis.opsForValue().get(metadataKey(searchKey));
                    if (metadata == null) {
                        redis.opsForZSet().remove(SEARCH_INDEX, searchKey);
                        continue;
                    }
                    try {
                        SearchKey query = objectMapper.readValue(metadata, SEARCH_KEY_TYPE);
                        if (matchesChange(query, event.previous(), event.current())) {
                            affectedSearches.add(searchKey);
                        }
                    } catch (JsonProcessingException exception) {
                        log.warn("Metadados inválidos no cache da busca {}; removendo a entrada", searchKey, exception);
                        affectedSearches.add(searchKey);
                    }
                }
            }

            removeSearchEntries(affectedSearches);
            redis.delete(List.of(DETAIL_PREFIX + id, relatedKey));
        } catch (DataAccessException exception) {
            log.warn("Redis indisponível ao invalidar o cache da vaga {}", id, exception);
        }
    }

    private boolean matchesChange(SearchKey query, JobSearchSnapshot previous, JobSearchSnapshot current) {
        return (previous != null && query.matches(previous)) || (current != null && query.matches(current));
    }

    private void removeSearchEntries(Set<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        List<String> redisKeys = keys.stream()
                .flatMap(key -> java.util.stream.Stream.of(key, metadataKey(key)))
                .toList();
        redis.delete(redisKeys);
        redis.opsForZSet().remove(SEARCH_INDEX, keys.toArray());
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (DataAccessException exception) {
            log.warn("Redis indisponível ao ler a chave {}", key, exception);
            return Optional.empty();
        } catch (JsonProcessingException exception) {
            log.warn("Valor inválido no cache Redis; removendo a chave {}", key, exception);
            try {
                redis.delete(key);
            } catch (DataAccessException ignored) {
                log.debug("Não foi possível remover a chave Redis inválida {}", key);
            }
            return Optional.empty();
        }
    }

    private void writeSearch(SearchCacheKey key, Object value, List<Long> jobIds) {
        try {
            long now = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore(SEARCH_INDEX, 0, now);
            redis.opsForValue().set(key.redisKey(), objectMapper.writeValueAsString(value), SEARCH_TTL);
            redis.opsForValue().set(metadataKey(key.redisKey()), objectMapper.writeValueAsString(key.query()), SEARCH_TTL);
            redis.opsForZSet().add(SEARCH_INDEX, key.redisKey(), now + SEARCH_TTL.toMillis());
            redis.expire(SEARCH_INDEX, SEARCH_INDEX_TTL);
            for (Long jobId : jobIds) {
                String indexKey = RELATED_SEARCH_PREFIX + jobId;
                redis.opsForSet().add(indexKey, key.redisKey());
                redis.expire(indexKey, SEARCH_TTL);
            }
        } catch (DataAccessException exception) {
            log.warn("Redis indisponível ao gravar o cache da busca", exception);
        } catch (JsonProcessingException exception) {
            log.warn("Não foi possível serializar a resposta da busca para cache", exception);
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (DataAccessException exception) {
            log.warn("Redis indisponível ao gravar a chave {}", key, exception);
        } catch (JsonProcessingException exception) {
            log.warn("Não foi possível serializar o valor para cache da chave {}", key, exception);
        }
    }

    private static String metadataKey(String searchKey) {
        return SEARCH_META_PREFIX + searchKey.substring(SEARCH_PREFIX.length());
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeState(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    public record SearchCacheKey(String redisKey, SearchKey query) {
    }

    public record SearchKey(
            String paginationMode,
            String queryText,
            String city,
            String state,
            WorkMode workMode,
            ContractType contractType,
            String technology,
            String salaryFrom,
            String salaryTo,
            int page,
            int size,
            String sortBy,
            String direction,
            String cursor
    ) {
        public boolean matches(JobSearchSnapshot job) {
            if (!job.active()) {
                return false;
            }
            if (queryText != null && !contains(job.title(), queryText)
                    && !contains(job.company(), queryText) && !contains(job.summary(), queryText)) {
                return false;
            }
            if (city != null && !city.equals(normalize(job.city()))) {
                return false;
            }
            if (state != null && !state.equals(normalizeState(job.state()))) {
                return false;
            }
            if (workMode != null && workMode != job.workMode()) {
                return false;
            }
            if (contractType != null && contractType != job.contractType()) {
                return false;
            }
            if (technology != null && job.technologies().stream().map(JobRedisCache::normalize)
                    .noneMatch(technology::equals)) {
                return false;
            }
            if (salaryFrom != null && (job.salaryMax() == null
                    || job.salaryMax().compareTo(new BigDecimal(salaryFrom)) < 0)) {
                return false;
            }
            return salaryTo == null || (job.salaryMin() != null
                    && job.salaryMin().compareTo(new BigDecimal(salaryTo)) <= 0);
        }

        private static boolean contains(String value, String query) {
            return normalize(value).contains(query);
        }
    }
}
