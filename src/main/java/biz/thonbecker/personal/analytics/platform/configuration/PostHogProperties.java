package biz.thonbecker.personal.analytics.platform.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "posthog")
public record PostHogProperties(boolean enabled, String apiKey, String apiHost, String projectToken) {

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(apiKey);
    }

    public boolean isBrowserConfigured() {
        return enabled && StringUtils.hasText(projectToken);
    }
}
