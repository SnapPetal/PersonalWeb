package biz.thonbecker.personal.user.api;

import java.time.Instant;

/** Published when a user requests a passwordless authentication link. */
public record UserLoginLinkRequestedEvent(String email, String loginUrl, Instant requestedAt) {}
