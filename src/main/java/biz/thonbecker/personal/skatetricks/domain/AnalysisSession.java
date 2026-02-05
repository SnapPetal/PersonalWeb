package biz.thonbecker.personal.skatetricks.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AnalysisSession {

    private final String sessionId;
    private final Instant startedAt;
    private final List<TrickAttempt> attempts;

    public AnalysisSession(String sessionId) {
        this.sessionId = sessionId;
        this.startedAt = Instant.now();
        this.attempts = new ArrayList<>();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<TrickAttempt> getAttempts() {
        return List.copyOf(attempts);
    }

    public void addAttempt(TrickAttempt attempt) {
        attempts.add(attempt);
    }
}
