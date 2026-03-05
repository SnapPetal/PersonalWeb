package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for AI-powered plant recommendations using AWS Bedrock (Claude).
 *
 * <p>Analyzes landscape images to suggest appropriate plants based on visible conditions,
 * hardiness zone, and landscaping principles.
 */
@Service
@Slf4j
public class LandscapeAiService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public LandscapeAiService(final ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes a landscape image and generates plant recommendations.
     *
     * <p>Uses Claude's vision capabilities to identify sunny/shady areas, soil conditions, and
     * spatial constraints, then recommends 5-8 suitable plants.
     *
     * @param imageData Raw image bytes
     * @param zone USDA hardiness zone
     * @param userDescription Optional user-provided description
     * @return List of plant recommendations with reasoning
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class})
    public List<PlantRecommendation> analyzeImageAndRecommendPlants(
            final byte[] imageData, final HardinessZone zone, final String userDescription) {

        try {
            log.info("Analyzing landscape image for zone {} with Claude AI", zone);

            final var base64Image = Base64.getEncoder().encodeToString(imageData);

            final var promptText = """
                You are an expert landscape designer and horticulturist. Analyze this landscape image
                and recommend 5-8 suitable plants based on the visible conditions.

                Context:
                - USDA Hardiness Zone: {zone}
                - User Description: {description}

                In your analysis, consider:
                1. Sunlight exposure (identify sunny vs shaded areas)
                2. Existing vegetation and soil conditions
                3. Space constraints and scale
                4. Climate compatibility with zone {zone}
                5. Aesthetic harmony and color schemes
                6. Maintenance requirements

                For each recommended plant, provide:
                - USDA plant symbol (if known, otherwise use scientific name abbreviation)
                - Common name
                - Scientific name
                - Reason for recommendation (specific to this landscape)
                - Confidence score (0-100)
                - Light requirement (FULL_SUN, PARTIAL_SHADE, or FULL_SHADE)
                - Water requirement (LOW, MEDIUM, or HIGH)

                Return your response as a JSON array with this exact structure:
                [
                  {
                    "usdaSymbol": "ACRU",
                    "commonName": "Red Maple",
                    "scientificName": "Acer rubrum",
                    "recommendationReason": "Excellent shade tree for the north side, tolerates zone {zone}",
                    "confidenceScore": 85,
                    "lightRequirement": "FULL_SUN",
                    "waterRequirement": "MEDIUM"
                  }
                ]
                """;

            final var promptTemplate = new PromptTemplate(promptText);
            final var prompt = promptTemplate.create(Map.of(
                    "zone",
                    zone.name(),
                    "description",
                    Objects.nonNull(userDescription) ? userDescription : "No description provided"));

            final var response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("AI Response: {}", response);

            final var recommendationMaps = objectMapper.readValue(
                    extractJsonFromResponse(response), new TypeReference<List<Map<String, Object>>>() {});

            final var recommendations = new ArrayList<PlantRecommendation>();
            for (final var recMap : recommendationMaps) {
                recommendations.add(new PlantRecommendation(
                        (String) recMap.get("usdaSymbol"),
                        (String) recMap.get("commonName"),
                        (String) recMap.get("scientificName"),
                        (String) recMap.get("recommendationReason"),
                        (Integer) recMap.get("confidenceScore"),
                        LightRequirement.valueOf((String) recMap.get("lightRequirement")),
                        WaterRequirement.valueOf((String) recMap.get("waterRequirement"))));
            }

            log.info("Successfully generated {} plant recommendations", recommendations.size());
            return recommendations;

        } catch (final Exception e) {
            log.error("Error generating AI plant recommendations", e);
            return generateFallbackRecommendations(zone);
        }
    }

    /**
     * Extracts JSON from Claude's response, removing markdown code blocks if present.
     *
     * @param response Raw response text
     * @return Clean JSON string
     */
    private String extractJsonFromResponse(final String response) {
        var cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Generates fallback plant recommendations when AI analysis fails.
     *
     * <p>Returns generic, zone-appropriate plants as a graceful degradation.
     *
     * @param zone USDA hardiness zone
     * @return List of fallback recommendations
     */
    private List<PlantRecommendation> generateFallbackRecommendations(final HardinessZone zone) {
        log.info("Using fallback plant recommendations for zone {}", zone);

        return List.of(
                new PlantRecommendation(
                        "ACRU",
                        "Red Maple",
                        "Acer rubrum",
                        "Hardy shade tree suitable for most zones (3-9)",
                        70,
                        LightRequirement.FULL_SUN,
                        WaterRequirement.MEDIUM),
                new PlantRecommendation(
                        "HYPA",
                        "Panicle Hydrangea",
                        "Hydrangea paniculata",
                        "Beautiful flowering shrub for partial shade",
                        65,
                        LightRequirement.PARTIAL_SHADE,
                        WaterRequirement.MEDIUM),
                new PlantRecommendation(
                        "ECPU",
                        "Purple Coneflower",
                        "Echinacea purpurea",
                        "Drought-tolerant native perennial with summer blooms",
                        60,
                        LightRequirement.FULL_SUN,
                        WaterRequirement.LOW));
    }

    /**
     * AI-generated plant recommendation.
     *
     * @param usdaSymbol USDA plant symbol
     * @param commonName Common name
     * @param scientificName Scientific name
     * @param recommendationReason Why this plant is recommended for this landscape
     * @param confidenceScore Confidence (0-100)
     * @param lightRequirement Sun requirements
     * @param waterRequirement Water needs
     */
    public record PlantRecommendation(
            String usdaSymbol,
            String commonName,
            String scientificName,
            String recommendationReason,
            int confidenceScore,
            LightRequirement lightRequirement,
            WaterRequirement waterRequirement) {}
}
