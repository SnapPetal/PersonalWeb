package biz.thonbecker.personal.analytics.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** Application event describing an analytics event that should be sent to PostHog. */
public record PostHogEvent(String distinctId, String eventName, Map<String, Object> properties) {

    public PostHogEvent {
        if (!StringUtils.hasText(distinctId)) {
            throw new IllegalArgumentException("PostHog distinct ID must not be blank");
        }
        if (!StringUtils.hasText(eventName)) {
            throw new IllegalArgumentException("PostHog event name must not be blank");
        }
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }
}
