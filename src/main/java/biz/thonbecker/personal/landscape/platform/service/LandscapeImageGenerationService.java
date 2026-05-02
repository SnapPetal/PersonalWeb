package biz.thonbecker.personal.landscape.platform.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating landscape images using Spring AI's OpenAI image model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeImageGenerationService {

    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 1024;

    private final ImageModel imageModel;

    @Value("${landscape.image-generation.model:${PERSONAL_OPENAI_IMAGE_MODEL:dall-e-3}}")
    private String imageModelName;

    /**
     * Generates a seasonal variation of a landscape image.
     *
     * @param landscapeImageData Original landscape image bytes, currently unused by Spring AI image generation
     * @param season Season name (Spring, Summer, Fall, Winter)
     * @param plantNames List of plant names in the landscape
     * @return Base64-encoded generated image, or null if generation fails
     */
    public String generateSeasonalImage(
            final byte[] landscapeImageData, final String season, final List<String> plantNames) {

        try {
            log.info("Generating {} landscape image with OpenAI", season);

            final var prompt = buildSeasonalPrompt(season, String.join(", ", plantNames));
            final var response = imageModel.call(new ImagePrompt(
                    prompt,
                    OpenAiImageOptions.builder()
                            .model(imageModelName)
                            .N(1)
                            .responseFormat("b64_json")
                            .width(IMAGE_WIDTH)
                            .height(IMAGE_HEIGHT)
                            .build()));

            final var result = response.getResult();
            if (result == null || result.getOutput() == null) {
                return null;
            }

            final var base64Image = result.getOutput().getB64Json();
            if (base64Image != null && !base64Image.isBlank()) {
                log.info("Successfully generated {} landscape image", season);
                return base64Image;
            }

            log.warn("OpenAI image generation returned no base64 image for {}", season);
            return null;

        } catch (final Exception e) {
            log.error("Failed to generate {} landscape image: {}", season, e.getMessage(), e);
            return null;
        }
    }

    private String buildSeasonalPrompt(final String season, final String plantList) {
        return switch (season.toLowerCase()) {
            case "spring" ->
                "Photorealistic residential landscape design rendering in early spring. Green grass lawn, trees with "
                        + "fresh light green leaves, and flowering plants beginning to bloom. Natural daylight. "
                        + "Include these plants in the yard: " + plantList + ".";
            case "summer" ->
                "Photorealistic residential landscape design rendering in summer. Green grass lawn, full leafy trees "
                        + "providing shade, flowers in bloom, bright sunny day. Include these plants in the yard: "
                        + plantList + ".";
            case "fall" ->
                "Photorealistic residential landscape design rendering in autumn. Green grass lawn with some brown "
                        + "patches, deciduous trees with orange and yellow leaves, some leaves on the ground, warm "
                        + "afternoon light. Include these plants in the yard: " + plantList + ".";
            case "winter" ->
                "Photorealistic residential landscape design rendering in winter. Dormant brown grass, bare deciduous "
                        + "trees, evergreen plants still green, overcast sky. Include these plants in the yard: "
                        + plantList + ".";
            default -> "Photorealistic residential landscape design rendering. Plants: " + plantList + ".";
        };
    }
}
