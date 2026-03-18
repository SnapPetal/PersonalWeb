package biz.thonbecker.personal.landscape.platform.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for generating landscape images using Amazon Nova Canvas via Bedrock.
 *
 * <p>Uses IMAGE_VARIATION to generate seasonal variations of a landscape photo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeImageGenerationService {

    private static final String MODEL_ID = "amazon.nova-canvas-v1:0";
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 1024;
    private static final int MAX_PIXELS = 4_194_304;

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    /**
     * Generates a seasonal variation of a landscape image.
     *
     * @param landscapeImageData Original landscape image bytes
     * @param season Season name (Spring, Summer, Fall, Winter)
     * @param plantNames List of plant names in the landscape
     * @return Base64-encoded generated image, or null if generation fails
     */
    public String generateSeasonalImage(
            final byte[] landscapeImageData, final String season, final List<String> plantNames) {

        try {
            log.info("Generating {} landscape image with Nova Canvas", season);

            final var resizedData = resizeIfNeeded(landscapeImageData);
            final var base64Image = Base64.getEncoder().encodeToString(resizedData);
            final var plantList = String.join(", ", plantNames);

            final var prompt = buildSeasonalPrompt(season, plantList);

            final var requestBody = Map.of(
                    "taskType",
                    "TEXT_IMAGE",
                    "textToImageParams",
                    Map.of(
                            "text",
                            prompt,
                            "conditionImage",
                            base64Image,
                            "controlMode",
                            "CANNY_EDGE",
                            "controlStrength",
                            0.8),
                    "imageGenerationConfig",
                    Map.of("width", IMAGE_WIDTH, "height", IMAGE_HEIGHT, "quality", "standard", "numberOfImages", 1));

            final var jsonBody = objectMapper.writeValueAsString(requestBody);

            final var request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .build();

            final var response = bedrockRuntimeClient.invokeModel(request);
            final var responseJson = response.body().asUtf8String();

            @SuppressWarnings("unchecked")
            final var responseMap = objectMapper.readValue(responseJson, Map.class);

            if (responseMap.containsKey("error")) {
                log.warn("Nova Canvas returned error for {} image: {}", season, responseMap.get("error"));
                return null;
            }

            @SuppressWarnings("unchecked")
            final var images = (List<String>) responseMap.get("images");
            if (images != null && !images.isEmpty()) {
                log.info("Successfully generated {} landscape image", season);
                return images.get(0);
            }

            return null;

        } catch (final Exception e) {
            log.error("Failed to generate {} landscape image: {}", season, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resizes the image if it exceeds Nova Canvas's 4,194,304 pixel limit.
     */
    private byte[] resizeIfNeeded(final byte[] imageData) {
        try {
            final var original = ImageIO.read(new ByteArrayInputStream(imageData));
            if (original == null) {
                return imageData;
            }

            final long pixels = (long) original.getWidth() * original.getHeight();
            if (pixels <= MAX_PIXELS) {
                return imageData;
            }

            // Scale down to fit within pixel limit
            final var scaleFactor = Math.sqrt((double) MAX_PIXELS / pixels);
            final var newWidth = (int) (original.getWidth() * scaleFactor);
            final var newHeight = (int) (original.getHeight() * scaleFactor);

            log.info(
                    "Resizing image from {}x{} ({} pixels) to {}x{} for Nova Canvas",
                    original.getWidth(),
                    original.getHeight(),
                    pixels,
                    newWidth,
                    newHeight);

            final var resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            final var g = resized.createGraphics();
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            final var baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            return baos.toByteArray();

        } catch (final Exception e) {
            log.warn("Failed to resize image, sending original: {}", e.getMessage());
            return imageData;
        }
    }

    private String buildSeasonalPrompt(final String season, final String plantList) {
        return switch (season.toLowerCase()) {
            case "spring" ->
                "Same residential house and yard in early spring. Green grass lawn, trees with fresh light green leaves, "
                        + "some flowering plants beginning to bloom. Natural daylight. "
                        + "Plants in the yard: " + plantList + ". Photorealistic photograph.";
            case "summer" ->
                "Same residential house and yard in summer. Green grass lawn, full leafy trees providing shade, "
                        + "flowers in bloom, bright sunny day. "
                        + "Plants in the yard: " + plantList + ". Photorealistic photograph.";
            case "fall" ->
                "Same residential house and yard in autumn. Green grass lawn with some brown patches, "
                        + "deciduous trees with orange and yellow leaves, some leaves on the ground. Warm afternoon light. "
                        + "Plants in the yard: " + plantList + ". Photorealistic photograph.";
            case "winter" ->
                "Same residential house and yard in winter. Dormant brown grass, bare deciduous trees, "
                        + "evergreen plants still green, overcast sky. "
                        + "Plants in the yard: " + plantList + ". Photorealistic photograph.";
            default -> "A residential house and yard. Plants: " + plantList + ". Photorealistic photograph.";
        };
    }
}
