package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.platform.client.UsdaPlantHttpClient;
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
 * <p>Looks up the ProfileImageFilename from the USDA search API, then constructs
 * the ImageLibrary thumbnail URL. Falls back to symbol-based URL guessing.
 * Results are cached for 24 hours.
 */
@Service
@Slf4j
public class PlantImageService {

    private static final String USDA_THUMBNAIL_BASE = "https://plants.sc.egov.usda.gov/ImageLibrary/thumbnail/";

    private final HttpClient httpClient;
    private final UsdaPlantHttpClient usdaClient;

    public PlantImageService(UsdaPlantHttpClient usdaClient) {
        this.usdaClient = usdaClient;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Gets the USDA plant image URL by USDA symbol.
     *
     * <p>First queries the USDA API for the ProfileImageFilename, then validates
     * the thumbnail URL exists and returns an actual image.
     *
     * @param usdaSymbol USDA plant symbol (e.g., "ACRU", "ECPU")
     * @return Image URL if the image exists, or null
     */
    @Cacheable(value = "plantImages", key = "#usdaSymbol")
    public String getPlantImageUrl(final String usdaSymbol) {
        if (usdaSymbol == null || usdaSymbol.isBlank()) {
            return null;
        }

        // Try to get the actual filename from the USDA API
        try {
            final var response = usdaClient.searchPlants(usdaSymbol, "Symbol", 1);
            if (response.plantResults() != null) {
                for (final var plant : response.plantResults()) {
                    if (usdaSymbol.equalsIgnoreCase(plant.symbol())
                            && plant.profileImageFilename() != null
                            && !plant.profileImageFilename().isBlank()) {
                        final var url = USDA_THUMBNAIL_BASE + plant.profileImageFilename();
                        if (imageExists(url)) {
                            log.info("Found USDA image for '{}': {}", usdaSymbol, url);
                            return url;
                        }
                    }
                }
            }
        } catch (final Exception e) {
            log.debug("Failed to look up profile image for '{}': {}", usdaSymbol, e.getMessage());
        }

        // Fall back to guessed symbol-based filenames
        final var symbol = usdaSymbol.toLowerCase().trim();
        for (final var suffix : new String[] {"_001_shp.jpg", "_001_php.jpg", "_001_tvp.jpg"}) {
            final var url = USDA_THUMBNAIL_BASE + symbol + suffix;
            if (imageExists(url)) {
                log.info("Found USDA image for '{}' via fallback: {}", usdaSymbol, url);
                return url;
            }
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
