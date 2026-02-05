package biz.thonbecker.personal.skatetricks.domain;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import java.time.Instant;
import java.util.List;

public record TrickAttempt(
        String sessionId,
        SupportedTrick trick,
        int confidence,
        int formScore,
        List<String> feedback,
        Instant createdAt) {}
