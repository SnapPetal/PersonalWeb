package biz.thonbecker.personal.user.api;

import java.time.Instant;

/**
 * Event published when a new user registers in the system.
 *
 * @param userId the unique user identifier
 * @param username the username
 * @param email the user's email
 * @param registeredAt the timestamp when the user registered
 */
public record UserRegisteredEvent(
        String userId, String username, String email, Instant registeredAt) {}
