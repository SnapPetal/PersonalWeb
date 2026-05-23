package biz.thonbecker.personal.shared.platform.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostHogProperties.class)
class PostHogConfig {}
