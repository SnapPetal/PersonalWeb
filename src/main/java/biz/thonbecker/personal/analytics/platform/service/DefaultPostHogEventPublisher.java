package biz.thonbecker.personal.analytics.platform.service;

import biz.thonbecker.personal.analytics.api.PostHogEvent;
import biz.thonbecker.personal.analytics.api.PostHogEventPublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DefaultPostHogEventPublisher implements PostHogEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(final String distinctId, final String eventName, final Map<String, Object> properties) {
        applicationEventPublisher.publishEvent(new PostHogEvent(distinctId, eventName, properties));
    }
}
