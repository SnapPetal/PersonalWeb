package biz.thonbecker.personal.user.api;

import java.time.Instant;

/** Published when a user ends an authenticated session. */
public record UserLoggedOutEvent(String userId, Instant loggedOutAt) {}
