package biz.thonbecker.personal.foosball.api;

import java.time.Instant;

/**
 * Domain event published when a new foosball player is created.
 */
public record PlayerCreatedEvent(String playerId, String playerName, Instant createdAt) {}
