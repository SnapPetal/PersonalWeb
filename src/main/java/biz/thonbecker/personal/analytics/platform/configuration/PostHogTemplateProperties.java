package biz.thonbecker.personal.analytics.platform.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes PostHog browser settings to Thymeleaf under a stable bean name. */
@Component("postHogProperties")
@RequiredArgsConstructor
public class PostHogTemplateProperties {

    private final PostHogProperties properties;

    public boolean isBrowserConfigured() {
        return properties.isBrowserConfigured();
    }

    public String getProjectToken() {
        return properties.projectToken();
    }

    public String getApiHost() {
        return properties.apiHost();
    }
}
