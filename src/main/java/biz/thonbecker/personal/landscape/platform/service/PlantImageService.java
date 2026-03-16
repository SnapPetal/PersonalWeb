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
 * <p>Uses the new USDA ImageLibrary thumbnail URL pattern. The old gallery URLs
 * ({@code plants.sc.egov.usda.gov/gallery/}) no longer serve images.
 * Results are cached for 24 hours.
 */
@Service
@Slf4j
public class PlantImageService {

    private static final String USDA_THUMBNAIL_URL =
            "https://plants.sc.egov.usda.gov/ImageLibrary/thumbnail/%s_001_shp.jpg";
    private static final String USDA_THUMBNAIL_ALT =
            "https://plants.sc.egov.usda.gov/ImageLibrary/thumbnail/%s_001_php.jpg";

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

        final var symbol = usdaSymbol.toLowerCase().trim();

        // Try the standard thumbnail first
        final var thumbnailUrl = String.format(USDA_THUMBNAIL_URL, symbol);
        if (imageExists(thumbnailUrl)) {
            log.info("Found USDA thumbnail for '{}': {}", usdaSymbol, thumbnailUrl);
            return thumbnailUrl;
        }

        // Try the publication thumbnail as fallback
        final var altUrl = String.format(USDA_THUMBNAIL_ALT, symbol);
        if (imageExists(altUrl)) {
            log.info("Found USDA alt thumbnail for '{}': {}", usdaSymbol, altUrl);
            return altUrl;
        }

        log.info("No USDA image found for '{}'", usdaSymbol);
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
            if (response.statusCode() != 200) {
                return false;
            }
            // USDA may return 200 with text/html for missing images
            final var contentType =
                    response.headers().firstValue("Content-Type").orElse("");
            return contentType.startsWith("image/");
        } catch (final Exception e) {
            log.debug("Failed to check image at {}: {}", url, e.getMessage());
            return false;
        }
    }
}
