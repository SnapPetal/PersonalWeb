package biz.thonbecker.personal.shared.platform.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "posthog")
public record PostHogProperties(boolean enabled, String apiKey, String apiHost) {

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(apiKey);
    }
}
