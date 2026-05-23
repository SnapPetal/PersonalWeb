package biz.thonbecker.personal.shared.platform.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class PostHogAnalyticsService {

    private static final String DEFAULT_API_HOST = "https://us.i.posthog.com";

    private final boolean enabled;
    private final String apiKey;
    private final String apiHost;
    private final WebClient webClient;

    public PostHogAnalyticsService(
            @Value("${posthog.enabled:false}") final boolean enabled,
            @Value("${posthog.api-key:}") final String apiKey,
            @Value("${posthog.api-host:https://us.i.posthog.com}") final String apiHost) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.apiHost = resolveApiHost(apiHost);
        this.webClient = WebClient.builder().baseUrl(this.apiHost).build();
    }

    @Async
    public void capture(final String distinctId, final String eventName, final Map<String, Object> properties) {
        if (!enabled || !StringUtils.hasText(apiKey) || Objects.isNull(distinctId) || distinctId.isBlank()) {
            return;
        }

        final var payload = new LinkedHashMap<String, Object>();
        payload.put("api_key", apiKey);
        payload.put("event", eventName);
        payload.put("distinct_id", distinctId);
        payload.put("properties", properties);
        payload.put("timestamp", Instant.now().toString());

        try {
            webClient
                    .post()
                    .uri("/capture/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (final Exception exception) {
            log.warn("PostHog capture failed for event {}", eventName, exception);
        }
    }

    private String resolveApiHost(final String apiHost) {
        if (!StringUtils.hasText(apiHost)) {
            return DEFAULT_API_HOST;
        }

        return apiHost;
    }
}
