package biz.thonbecker.personal.skatetricks.platform;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Generates vector embeddings via Spring AI's EmbeddingModel (backed by AWS Bedrock Titan Text Embeddings V2).
 * Used by the skatetricks module to embed pose data for storage and retrieval
 * in the S3 Vectors store.
 */
@Component
@Slf4j
class EmbeddingService {

    private static final int MAX_TEXT_LENGTH = 4000;

    private final EmbeddingModel embeddingModel;

    EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Embeds the given text into a float vector using the configured embedding model.
     *
     * @param text the text to embed
     * @return a list of floats representing the embedding
     */
    List<Float> embed(final String text) {
        final var truncated = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
        final var embedding = embeddingModel.embed(truncated);
        final var result = new java.util.ArrayList<Float>(embedding.length);
        for (final var val : embedding) {
            result.add(val);
        }
        return result;
    }
}
