package biz.thonbecker.personal.landscape.platform.service;

import static java.util.Objects.nonNull;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.SeasonalAnalysis;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

/**
 * Service for AI-powered plant recommendations using OpenAI.
 *
 * <p>Analyzes landscape images to suggest appropriate plants based on visible conditions,
 * hardiness zone, and landscaping principles.
 */
@Service
@Slf4j
public class LandscapeAiService {

    private final ChatClient chatClient;

    public LandscapeAiService(final ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Analyzes a landscape image and generates plant recommendations.
     *
     * <p>Uses OpenAI vision capabilities to identify sunny/shady areas, soil conditions, and
     * spatial constraints, then recommends 5-8 suitable plants.
     *
     * @param imageData Raw image bytes
     * @param zone USDA hardiness zone
     * @param userDescription Optional user-provided description
     * @return List of plant recommendations with reasoning
     */
    @Retryable(
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class})
    public List<PlantRecommendation> analyzeImageAndRecommendPlants(
            final byte[] imageData, final HardinessZone zone, final String userDescription) {

        try {
            log.info("Analyzing landscape image for zone {} with OpenAI", zone);

            final var description = nonNull(userDescription) ? userDescription : "No description provided";

            final var imageMedia = Media.builder()
                    .mimeType(detectImageMimeType(imageData))
                    .data(imageData)
                    .build();

            final var recommendations = chatClient
                    .prompt()
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .user(u -> u.text("""
                            You are an expert landscape designer and horticulturist. Analyze this landscape image
                            and recommend 5-8 suitable plants based on the visible conditions. Return the plants
                            in the recommendations field.

                            Context:
                            - USDA Hardiness Zone: {zone}
                            - User Description: {description}

                            In your analysis, consider:
                            1. Sunlight exposure (identify sunny vs shaded areas)
                            2. Existing vegetation and soil conditions
                            3. Space constraints and scale
                            4. Climate compatibility with the specified hardiness zone
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
                            """)
                            .param("zone", zone.name())
                            .param("description", description)
                            .media(imageMedia))
                    .call()
                    .entity(PlantRecommendationBatch.class)
                    .recommendations();

            log.info("Successfully generated {} plant recommendations", recommendations.size());
            return recommendations;

        } catch (final Exception e) {
            log.error("Error generating AI plant recommendations", e);
            return generateFallbackRecommendations(zone);
        }
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
     * Analyzes a landscape with its planted plants and describes how it would look across seasons.
     *
     * @param imageData Raw landscape image bytes
     * @param zone USDA hardiness zone
     * @param plantDescriptions List of plant names and positions placed on the landscape
     * @return Seasonal analysis with descriptions for each season
     */
    @Retryable(
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class})
    public SeasonalAnalysis analyzeSeasons(
            final byte[] imageData, final HardinessZone zone, final List<String> plantDescriptions) {

        log.info("Generating seasonal analysis for zone {} with {} plants", zone, plantDescriptions.size());

        final var plantList = String.join("\n- ", plantDescriptions);

        final var imageMedia = Media.builder()
                .mimeType(detectImageMimeType(imageData))
                .data(imageData)
                .build();

        final var analysis = chatClient
                .prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(u -> u.text("""
                        You are an expert landscape designer. Look at this landscape image. The following plants \
                        have been placed in this yard:

                        - {plantList}

                        USDA Hardiness Zone: {zone}

                        For each of the four seasons (Spring, Summer, Fall, Winter), provide:
                        1. A vivid 2-3 sentence description of how this landscape would look with these plants \
                        in that season. Describe colors, textures, blooms, foliage changes, and overall feel. \
                        Be specific to the actual plants listed.
                        2. A list of 2-3 care tips specific to that season for these plants.
                        """)
                        .param("plantList", plantList)
                        .param("zone", zone.name())
                        .media(imageMedia))
                .call()
                .entity(SeasonalTextAnalysis.class);

        return new SeasonalAnalysis(
                toSeasonalDescription(analysis.spring()),
                toSeasonalDescription(analysis.summer()),
                toSeasonalDescription(analysis.fall()),
                toSeasonalDescription(analysis.winter()));
    }

    private static SeasonalAnalysis.SeasonalDescription toSeasonalDescription(
            final SeasonalDescriptionText description) {
        if (description == null) {
            return null;
        }
        return new SeasonalAnalysis.SeasonalDescription(description.description(), description.careTips(), null);
    }

    private static org.springframework.util.MimeType detectImageMimeType(final byte[] imageData) {
        if (imageData.length >= 8
                && imageData[0] == (byte) 0x89
                && imageData[1] == 0x50
                && imageData[2] == 0x4E
                && imageData[3] == 0x47
                && imageData[4] == 0x0D
                && imageData[5] == 0x0A
                && imageData[6] == 0x1A
                && imageData[7] == 0x0A) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        return MimeTypeUtils.IMAGE_JPEG;
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

    private record PlantRecommendationBatch(List<PlantRecommendation> recommendations) {}

    private record SeasonalTextAnalysis(
            SeasonalDescriptionText spring,
            SeasonalDescriptionText summer,
            SeasonalDescriptionText fall,
            SeasonalDescriptionText winter) {}

    private record SeasonalDescriptionText(String description, List<String> careTips) {}
}
