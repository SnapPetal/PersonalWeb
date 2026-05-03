package biz.thonbecker.personal.landscape.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating landscape images using OpenAI image editing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeImageGenerationService {

    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

    @Value("${landscape.image-generation.responses-model:${PERSONAL_OPENAI_IMAGE_RESPONSES_MODEL:gpt-4.1-mini}}")
    private String imageResponsesModelName;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private volatile boolean imageGenerationAccessDenied;

    /**
     * Generates a seasonal variation by editing the original landscape image.
     *
     * @param landscapeImageData Original landscape image bytes
     * @param season Season name (Spring, Summer, Fall, Winter)
     * @param placements Plant placement instructions with normalized image coordinates
     * @return Base64-encoded generated image, or null if generation fails
     */
    public String generateSeasonalImage(
            final byte[] landscapeImageData, final String season, final List<PlantPlacementPrompt> placements) {

        try {
            if (Objects.isNull(openAiApiKey) || openAiApiKey.isBlank()) {
                log.warn("Skipping {} landscape image edit because no OpenAI API key is configured", season);
                return null;
            }
            if (imageGenerationAccessDenied) {
                log.debug(
                        "Skipping {} landscape image edit because OpenAI image generation access is unavailable",
                        season);
                return null;
            }

            log.info("Generating {} landscape image edit with OpenAI", season);
            final var requestBody = objectMapper.writeValueAsString(
                    buildImageGenerationRequestBody(landscapeImageData, buildSeasonalEditPrompt(season, placements)));

            final var request = HttpRequest.newBuilder()
                    .uri(RESPONSES_URI)
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                handleFailedResponse(season, response.statusCode(), response.body());
                return null;
            }

            final var responseJson = objectMapper.readTree(response.body());
            final var base64Image = extractGeneratedImage(responseJson);

            if (Objects.nonNull(base64Image) && !base64Image.isBlank()) {
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

    private void handleFailedResponse(final String season, final int statusCode, final String responseBody) {
        if (statusCode == 403 && responseBody.contains("organization must be verified")) {
            imageGenerationAccessDenied = true;
            log.warn(
                    "OpenAI image generation is unavailable for this organization. "
                            + "Seasonal previews will use the local plan image fallback until the organization is "
                            + "verified. OpenAI response for {}: {}",
                    season,
                    responseBody);
            return;
        }

        log.error("OpenAI image edit failed for {}: status={}, body={}", season, statusCode, responseBody);
    }

    private Map<String, Object> buildImageGenerationRequestBody(final byte[] landscapeImageData, final String prompt) {
        final var textContent = new LinkedHashMap<String, Object>();
        textContent.put("type", "input_text");
        textContent.put("text", prompt);

        final var imageContent = new LinkedHashMap<String, Object>();
        imageContent.put("type", "input_image");
        imageContent.put("image_url", toDataUrl(landscapeImageData));

        final var inputMessage = new LinkedHashMap<String, Object>();
        inputMessage.put("role", "user");
        inputMessage.put("content", List.of(textContent, imageContent));

        final var imageTool = new LinkedHashMap<String, Object>();
        imageTool.put("type", "image_generation");

        final var body = new LinkedHashMap<String, Object>();
        body.put("model", imageResponsesModelName);
        body.put("input", List.of(inputMessage));
        body.put("tools", List.of(imageTool));
        return body;
    }

    private String extractGeneratedImage(final com.fasterxml.jackson.databind.JsonNode responseJson) {
        final var output = responseJson.path("output");
        if (!output.isArray()) {
            return null;
        }

        for (final var item : output) {
            if ("image_generation_call".equals(item.path("type").asText())) {
                return item.path("result").asText(null);
            }
        }

        return null;
    }

    private String toDataUrl(final byte[] imageData) {
        return detectImageMimeType(imageData) + ";base64," + Base64.getEncoder().encodeToString(imageData);
    }

    private static String detectImageMimeType(final byte[] imageData) {
        if (imageData.length >= 8
                && imageData[0] == (byte) 0x89
                && imageData[1] == 0x50
                && imageData[2] == 0x4E
                && imageData[3] == 0x47
                && imageData[4] == 0x0D
                && imageData[5] == 0x0A
                && imageData[6] == 0x1A
                && imageData[7] == 0x0A) {
            return "data:image/png";
        }
        return "data:image/jpeg";
    }

    private String buildSeasonalEditPrompt(final String season, final List<PlantPlacementPrompt> placements) {
        final var plantInstructions = placements.isEmpty()
                ? "- No new plants are placed; only adjust the existing yard for the season."
                : placements.stream()
                        .map(placement -> String.format(
                                "- Add %s (%s) at x=%.1f%% from the left edge and y=%.1f%% from the top edge. "
                                        + "Treat this coordinate as the trunk/root base location where the plant "
                                        + "emerges from the ground. Render the entire plant from ground contact to "
                                        + "full canopy or top growth, scaled and lit as if it is planted in this yard.",
                                placement.displayName(),
                                placement.usdaSymbol(),
                                placement.xPercent(),
                                placement.yPercent()))
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("");

        return """
                Edit this exact uploaded residential yard photo into a single realistic %s landscape preview image.

                Preserve the original house, roofline, driveway, windows, street, camera angle, perspective, image
                composition, and property layout. Do not invent a different house or a different yard. Keep unchanged
                areas visually consistent with the input photo.

                Add the planned plants at these precise normalized image locations. For trees, the coordinate is the
                trunk base/root flare where the tree enters the ground, not the center of the canopy:
                %s

                Seasonal appearance instructions:
                %s

                Generate one cohesive image containing all of the listed plant placements together in the same yard.
                Make the result photorealistic. Render each newly added tree or plant as a full-size landscape element
                planted in the ground of the house/yard: visible trunk or stems, complete canopy/top growth, natural
                root contact, realistic scale, perspective, shadows, occlusion, and lighting. Do not render thumbnails,
                icons, stickers, circular plant photos, floating cutouts, labels, map pins, diagram markers, text, or
                UI elements.
                """.formatted(season, plantInstructions, seasonalStyle(season));
    }

    private String seasonalStyle(final String season) {
        return switch (season.toLowerCase()) {
            case "spring" ->
                "Early spring: fresh green grass, budding trees, soft new leaves, emerging flowers, clear natural "
                        + "daylight, and lightly refreshed planting beds.";
            case "summer" ->
                "Summer: full green lawn, mature full foliage, strong healthy leaves, flowering plants in bloom, and "
                        + "bright warm daylight.";
            case "fall" ->
                "Autumn: orange, gold, and red deciduous foliage where appropriate, some fallen leaves, slightly "
                        + "warmer afternoon light, and seasonal but tidy planting beds.";
            case "winter" ->
                "Winter: dormant grass, bare deciduous branches where appropriate, evergreens still green, subtle "
                        + "cold overcast light, and no heavy snow unless already present in the input image.";
            default -> "Natural realistic seasonal residential landscaping.";
        };
    }

    public record PlantPlacementPrompt(String usdaSymbol, String displayName, double xPercent, double yPercent) {}
}
