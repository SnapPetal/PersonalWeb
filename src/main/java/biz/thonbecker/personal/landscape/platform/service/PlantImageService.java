package biz.thonbecker.personal.landscape.platform.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching plant images from the USDA Plants Database.
 *
 * <p>Uses the USDA gallery URL pattern with the plant's USDA symbol.
 * Results are cached for 24 hours.
 */
@Service
@Slf4j
public class PlantImageService {

    private static final String USDA_IMAGE_URL = "https://plants.sc.egov.usda.gov/gallery/standard/%s_001_shp.jpg";
    private static final String USDA_IMAGE_ALT = "https://plants.sc.egov.usda.gov/gallery/pubs/%s_001_php.jpg";

    private final HttpClient httpClient;

    public PlantImageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Gets the USDA plant image URL by USDA symbol.
     *
     * @param usdaSymbol USDA plant symbol (e.g., "ACRU", "ECPU")
     * @return Image URL if the image exists, or null
     */
    @Cacheable(value = "plantImages", key = "#usdaSymbol")
    public String getPlantImageUrl(final String usdaSymbol) {
        if (usdaSymbol == null || usdaSymbol.isBlank()) {
            return null;
        }

        final var symbol = usdaSymbol.toUpperCase().trim();

        // Try the standard gallery image first
        final var standardUrl = String.format(USDA_IMAGE_URL, symbol);
        if (imageExists(standardUrl)) {
            log.info("Found USDA image for '{}': {}", symbol, standardUrl);
            return standardUrl;
        }

        // Try the publication gallery as fallback
        final var altUrl = String.format(USDA_IMAGE_ALT, symbol);
        if (imageExists(altUrl)) {
            log.info("Found USDA alt image for '{}': {}", symbol, altUrl);
            return altUrl;
        }

        log.info("No USDA image found for '{}'", symbol);
        return null;
    }

    private boolean imageExists(final String url) {
        try {
            final var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(3))
                    .build();

            final var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (final Exception e) {
            log.debug("Failed to check image at {}: {}", url, e.getMessage());
            return false;
        }
    }
}
