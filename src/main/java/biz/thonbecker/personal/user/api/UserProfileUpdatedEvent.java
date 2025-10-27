package biz.thonbecker.personal.user.api;

import java.time.Instant;

/**
 * Event published when a user's profile is updated.
 *
 * @param userId the user ID whose profile was updated
 * @param displayName the new display name
 * @param updatedAt the timestamp when the profile was updated
 */
public record UserProfileUpdatedEvent(String userId, String displayName, Instant updatedAt) {}
