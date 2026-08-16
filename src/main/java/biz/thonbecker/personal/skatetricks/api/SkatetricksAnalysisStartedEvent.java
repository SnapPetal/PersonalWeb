package biz.thonbecker.personal.skatetricks.api;

public record SkatetricksAnalysisStartedEvent(
        String distinctId, String mode, Integer frameCount, Integer fileSizeBytes) {}
