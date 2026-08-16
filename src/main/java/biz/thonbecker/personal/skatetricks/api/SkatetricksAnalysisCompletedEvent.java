package biz.thonbecker.personal.skatetricks.api;

public record SkatetricksAnalysisCompletedEvent(String distinctId, String mode, Long attemptId, String trick) {}
