package biz.thonbecker.personal.analytics.api;

import java.util.Map;

/** Publishes application analytics events without coupling callers to the PostHog client. */
public interface PostHogEventPublisher {

    void publish(String distinctId, String eventName, Map<String, Object> properties);
}
