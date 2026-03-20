package biz.thonbecker.personal.notification.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
record NotificationProperties(boolean enabled, String sender, String admin) {}
