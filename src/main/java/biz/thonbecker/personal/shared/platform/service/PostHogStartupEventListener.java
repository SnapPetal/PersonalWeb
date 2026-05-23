package biz.thonbecker.personal.shared.platform.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class PostHogStartupEventListener {

    private final PostHogAnalyticsService postHogAnalyticsService;

    @EventListener(ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("Sending PostHog startup event");
        postHogAnalyticsService.capture(
                "system", "app_started", Map.of("component", "personal-web", "source", "application_ready_event"));
    }
}
