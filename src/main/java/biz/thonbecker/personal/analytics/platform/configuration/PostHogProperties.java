package biz.thonbecker.personal.analytics.platform.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "posthog")
@Component("postHogProperties")
public record PostHogProperties(boolean enabled, String apiKey, String apiHost, String projectToken) {

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(apiKey);
    }

    public boolean isBrowserConfigured() {
        return enabled && StringUtils.hasText(projectToken);
    }
}
