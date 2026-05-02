package biz.thonbecker.personal.skatetricks.platform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Schema for the model's JSON response about skateboard trick analysis.
 * Used with Spring AI's BeanOutputConverter for type-safe structured output.
 * The model may include extra fields like "reasoning" — these are safely ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TrickAnalysisResponseSchema(
        String trick,
        int confidence,
        int formScore,
        List<String> feedback,
        List<TrickSequenceEntrySchema> trickSequence) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrickSequenceEntrySchema(String trick, String timeframe, int confidence) {}
}
