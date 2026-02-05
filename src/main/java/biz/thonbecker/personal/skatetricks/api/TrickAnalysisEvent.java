package biz.thonbecker.personal.skatetricks.api;

import java.time.Instant;

public record TrickAnalysisEvent(String sessionId, TrickAnalysisResult result, Instant analyzedAt) {}
