package biz.thonbecker.personal.analytics.platform.service;

import biz.thonbecker.personal.analytics.platform.configuration.PostHogProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Evaluates PostHog feature flags for server-side code. */
@Service
@Slf4j
public class PostHogFeatureFlagService {

    private static final Duration CACHE_DURATION = Duration.ofMinutes(1);

    private final PostHogProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final Map<String, CachedFlags> cache = new ConcurrentHashMap<>();

    public PostHogFeatureFlagService(
            final PostHogProperties properties,
            final ObjectMapper objectMapper,
            final WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(properties.apiHost()).build();
    }

    /** Returns false when PostHog is disabled, unavailable, or the flag is absent. */
    public boolean isEnabled(final String flagKey, final String distinctId) {
        if (!StringUtils.hasText(flagKey) || !StringUtils.hasText(distinctId)) {
            return false;
        }

        final var flags = getFlags(distinctId);
        final var value = flags.get(flagKey);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    /** Returns the selected multivariate value, or empty when the flag is disabled or absent. */
    public Optional<String> getVariant(final String flagKey, final String distinctId) {
        if (!StringUtils.hasText(flagKey) || !StringUtils.hasText(distinctId)) {
            return Optional.empty();
        }

        final var value = getFlags(distinctId).get(flagKey);
        return value instanceof String stringValue ? Optional.of(stringValue) : Optional.empty();
    }

    /** Returns the evaluated flag values for a user, with an empty result as the safe fallback. */
    public Map<String, Object> getFlags(final String distinctId) {
        if (!properties.isConfigured() || !StringUtils.hasText(distinctId)) {
            return Map.of();
        }

        final var cached = cache.get(distinctId);
        if (Objects.nonNull(cached) && cached.expiresAt().isAfter(Instant.now())) {
            return cached.flags();
        }

        try {
            final var response = webClient
                    .post()
                    .uri("/decide/?v=4")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("api_key", properties.apiKey(), "distinct_id", distinctId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(5));
            final var flags = parseFlags(response);
            cache.put(distinctId, new CachedFlags(flags, Instant.now().plus(CACHE_DURATION)));
            return flags;
        } catch (final Exception exception) {
            log.warn("PostHog feature flag evaluation failed for distinct ID {}", distinctId, exception);
            return Map.of();
        }
    }

    private Map<String, Object> parseFlags(final JsonNode response) {
        if (Objects.isNull(response)) {
            return Map.of();
        }

        final var featureFlags =
                response.has("featureFlags") ? response.get("featureFlags") : response.get("feature_flags");
        if (Objects.isNull(featureFlags) || !featureFlags.isObject()) {
            return Map.of();
        }

        final var flags = new java.util.LinkedHashMap<String, Object>();
        featureFlags.fields().forEachRemaining(entry -> flags.put(entry.getKey(), flagValue(entry.getValue())));
        return Map.copyOf(flags);
    }

    private Object flagValue(final JsonNode value) {
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        return objectMapper.convertValue(value, Object.class);
    }

    private record CachedFlags(Map<String, Object> flags, Instant expiresAt) {}
}
