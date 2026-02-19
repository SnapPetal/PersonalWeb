package biz.thonbecker.personal.skatetricks.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Generates vector embeddings via AWS Bedrock Titan Text Embeddings V2.
 * Used by the skatetricks module to embed trick analyses for storage and retrieval
 * in the S3 Vectors store.
 */
@Component
@Slf4j
class EmbeddingService {

    private static final String TITAN_EMBED_MODEL = "amazon.titan-embed-text-v2:0";
    private static final int DIMENSIONS = 1024;
    private static final int MAX_TEXT_LENGTH = 4000;

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    EmbeddingService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Embeds the given text into a float vector using Titan Text Embeddings V2.
     *
     * @param text the text to embed
     * @return a list of floats representing the embedding
     */
    List<Float> embed(final String text) {
        try {
            final var truncated = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
            final var requestBody = """
                    {"inputText": %s, "dimensions": %d, "normalize": true}
                    """.formatted(objectMapper.writeValueAsString(truncated), DIMENSIONS);

            final var response = bedrockRuntimeClient.invokeModel(r -> r.modelId(TITAN_EMBED_MODEL)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBody)));

            final var responseNode = objectMapper.readTree(response.body().asByteArray());
            final var embeddingArray = responseNode.get("embedding");

            final var embedding = new ArrayList<Float>(DIMENSIONS);
            for (final var val : embeddingArray) {
                embedding.add(val.floatValue());
            }
            return embedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding for text (length={})", text.length(), e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
}
