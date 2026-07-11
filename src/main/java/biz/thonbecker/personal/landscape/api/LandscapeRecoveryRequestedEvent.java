package biz.thonbecker.personal.landscape.api;

public record LandscapeRecoveryRequestedEvent(String email, String recoveryUrl) {}
