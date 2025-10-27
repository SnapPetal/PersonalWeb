package biz.thonbecker.personal.user.api;

import java.time.Instant;

/**
 * Event published when a user logs in.
 *
 * @param userId the user ID who logged in
 * @param username the username
 * @param loginAt the timestamp when the user logged in
 */
public record UserLoginEvent(String userId, String username, Instant loginAt) {}
