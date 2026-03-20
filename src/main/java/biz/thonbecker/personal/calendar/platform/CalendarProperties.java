package biz.thonbecker.personal.calendar.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "calendar.nextcloud")
record CalendarProperties(
        boolean enabled,
        String baseUrl,
        String username,
        String password,
        String calendarName,
        String organizerEmail) {}
