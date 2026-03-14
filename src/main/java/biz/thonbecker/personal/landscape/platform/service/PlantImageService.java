package biz.thonbecker.personal.landscape.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching plant images from the Wikipedia REST API.
 *
 * <p>Looks up plant pages by scientific name and returns the thumbnail image URL.
 * Results are cached for 24 hours.
 */
@Service
@Slf4j
public class PlantImageService {

    private static final String WIKIPEDIA_API = "https://en.wikipedia.org/api/rest_v1/page/summary/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PlantImageService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetches a plant image URL from Wikipedia by scientific or common name.
     *
     * @param plantName Scientific name or common name of the plant
     * @return Thumbnail image URL, or null if not found
     */
    @Cacheable(value = "plantImages", key = "#plantName")
    public String getPlantImageUrl(final String plantName) {
        try {
            // Try scientific name first, then common name
            var imageUrl = fetchWikipediaImage(plantName);
            if (imageUrl == null && plantName.contains(" ")) {
                // Try with underscores for multi-word names
                imageUrl = fetchWikipediaImage(plantName.replace(" ", "_"));
            }
            return imageUrl;
        } catch (final Exception e) {
            log.debug("Failed to fetch plant image for '{}': {}", plantName, e.getMessage());
            return null;
        }
    }

    private String fetchWikipediaImage(final String name) {
        try {
            final var encodedName = name.replace(" ", "_");
            final var uri = URI.create(WIKIPEDIA_API + encodedName);

            final var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "PersonalWeb/1.0 (thonbecker.com)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            final var json = objectMapper.readTree(response.body());

            // Check for thumbnail
            final var thumbnail = json.path("thumbnail");
            if (!thumbnail.isMissingNode()) {
                final var source = thumbnail.path("source");
                if (!source.isMissingNode()) {
                    log.debug("Found Wikipedia image for '{}': {}", name, source.asText());
                    return source.asText();
                }
            }

            // Check for originalimage as fallback
            final JsonNode originalImage = json.path("originalimage");
            if (!originalImage.isMissingNode()) {
                final var source = originalImage.path("source");
                if (!source.isMissingNode()) {
                    return source.asText();
                }
            }

            return null;

        } catch (final Exception e) {
            log.debug("Wikipedia API error for '{}': {}", name, e.getMessage());
            return null;
        }
    }
}
