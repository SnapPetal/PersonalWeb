package biz.thonbecker.personal.skatetricks.api;

import java.util.List;

public record TrickAnalysisResult(
        SupportedTrick trick,
        int confidence,
        int formScore,
        List<String> feedback,
        List<TrickSequenceEntry> trickSequence,
        Long attemptId) {

    public TrickAnalysisResult(
            SupportedTrick trick,
            int confidence,
            int formScore,
            List<String> feedback,
            List<TrickSequenceEntry> trickSequence) {
        this(trick, confidence, formScore, feedback, trickSequence, null);
    }

    public TrickAnalysisResult(SupportedTrick trick, int confidence, int formScore, List<String> feedback) {
        this(trick, confidence, formScore, feedback, List.of(), null);
    }

    public TrickAnalysisResult withAttemptId(Long attemptId) {
        return new TrickAnalysisResult(trick, confidence, formScore, feedback, trickSequence, attemptId);
    }
}
