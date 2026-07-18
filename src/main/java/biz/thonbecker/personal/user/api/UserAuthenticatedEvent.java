package biz.thonbecker.personal.user.api;

import java.time.Instant;

/** Published after a passwordless authentication link has been redeemed. */
public record UserAuthenticatedEvent(String userId, String email, Instant authenticatedAt) {}
