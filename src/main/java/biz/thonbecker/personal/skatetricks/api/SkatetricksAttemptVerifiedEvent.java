package biz.thonbecker.personal.skatetricks.api;

public record SkatetricksAttemptVerifiedEvent(String distinctId, Long attemptId, boolean corrected) {}
