package biz.thonbecker.personal.analytics.platform.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostHogProperties.class)
class PostHogConfig {}
