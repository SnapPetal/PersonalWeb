# Analytics and PostHog

Analytics follows the same event-driven boundary as the other Spring Modulith modules:

```text
feature module → feature-owned api event → PostHogEventListener → PostHog
```

Feature modules own the event types and their payloads. They publish those events through
`ApplicationEventPublisher`; they do not depend on PostHog, analytics event names, or an analytics
publisher interface.

The Analytics module listens asynchronously through `PostHogEventListener`. That listener maps
feature-owned events to PostHog event names and provider properties. This keeps provider-specific
details in the Analytics module and allows PostHog to be replaced without changing feature modules.

The listener currently handles events from Booking, Foosball, Landscape, SkateTricks, Trivia, and
User. Page views, startup telemetry, and feature-flag evaluation are also implemented inside the
Analytics module because they are provider/infrastructure concerns rather than feature-domain events.

PostHog delivery is best-effort and disabled when PostHog is not configured. Analytics failures do
not change the outcome of the originating feature operation.
