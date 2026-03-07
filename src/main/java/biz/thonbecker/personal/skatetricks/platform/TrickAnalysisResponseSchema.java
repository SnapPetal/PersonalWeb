package biz.thonbecker.personal.skatetricks.platform;

import java.util.List;

/**
 * Schema for Claude's JSON response about skateboard trick analysis.
 * Used with Spring AI's BeanOutputConverter for type-safe structured output.
 * This ensures Claude returns properly formatted JSON that matches our expected structure.
 */
record TrickAnalysisResponseSchema(
        String trick,
        int confidence,
        int formScore,
        List<String> feedback,
        List<TrickSequenceEntrySchema> trickSequence) {

    record TrickSequenceEntrySchema(String trick, String timeframe, int confidence) {}
}
