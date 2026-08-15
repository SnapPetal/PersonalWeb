/**
 * Analytics integration module.
 *
 * <p>Business modules publish analytics events through the small API exported by this module.
 * PostHog configuration and delivery remain internal implementation details.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Analytics",
        allowedDependencies = {"shared", "booking :: api", "foosball :: api", "trivia :: api", "user :: api"})
package biz.thonbecker.personal.analytics;
