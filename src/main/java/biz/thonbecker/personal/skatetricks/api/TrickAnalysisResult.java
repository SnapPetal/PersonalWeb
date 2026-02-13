package biz.thonbecker.personal.skatetricks.api;

import java.util.List;

public record TrickAnalysisResult(
        SupportedTrick trick,
        int confidence,
        int formScore,
        List<String> feedback,
        List<TrickSequenceEntry> trickSequence) {

    public TrickAnalysisResult(SupportedTrick trick, int confidence, int formScore, List<String> feedback) {
        this(trick, confidence, formScore, feedback, List.of());
    }
}
