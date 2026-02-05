package biz.thonbecker.personal.skatetricks.api;

import java.util.List;

public record TrickAnalysisResult(SupportedTrick trick, int confidence, int formScore, List<String> feedback) {}
