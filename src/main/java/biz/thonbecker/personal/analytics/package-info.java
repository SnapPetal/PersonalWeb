/**
 * Analytics integration module.
 *
 * <p>Feature modules publish module-owned domain or application events from their exported {@code api}
 * packages. The analytics module listens to those events and translates them into PostHog events.
 * PostHog configuration, event delivery, and provider-specific event names remain internal
 * implementation details.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Analytics",
        allowedDependencies = {
            "shared",
            "booking :: api",
            "foosball :: api",
            "landscape :: api",
            "skatetricks :: api",
            "trivia :: api",
            "user :: api"
        })
package biz.thonbecker.personal.analytics;
